pub mod meter;

use std::io;

use anyhow::Result;
use crossterm::event::{Event, EventStream, KeyCode, KeyEventKind, KeyModifiers};
use crossterm::terminal::{self, EnterAlternateScreen, LeaveAlternateScreen};
use crossterm::{cursor, execute};
use futures::StreamExt;
use ratatui::backend::CrosstermBackend;
use ratatui::layout::{Constraint, Direction, Layout, Rect};
use ratatui::style::{Color, Modifier, Style};
use ratatui::text::{Line, Span};
use ratatui::symbols::bar;
use ratatui::widgets::{Block, Borders, Paragraph, Tabs};
use ratatui::Terminal;
use tokio::sync::{mpsc, watch};

use blender_proto::param::ParamId;
use blender_proto::state::{MixerState, NUM_BUSES, NUM_INPUTS};
use blender_proto::tuple::Tuple;

use meter::AudioMeter;

/// A visible column in the per-bus view.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum Column {
    Input(usize), // 0–5
    Level,
    Compressor,
    MicGain,
}

impl Column {
    fn param_id(self) -> ParamId {
        match self {
            Column::Input(0) => ParamId::Input1,
            Column::Input(1) => ParamId::Input2,
            Column::Input(2) => ParamId::Input3,
            Column::Input(3) => ParamId::Input4,
            Column::Input(4) => ParamId::Input5,
            Column::Input(5) => ParamId::Input6,
            Column::Input(_) => unreachable!(),
            Column::Level => ParamId::Level,
            Column::Compressor => ParamId::Compressor,
            Column::MicGain => ParamId::MicGain,
        }
    }

    fn label(self) -> &'static str {
        match self {
            Column::Input(n) => match n {
                0 => "IN 1",
                1 => "IN 2",
                2 => "IN 3",
                3 => "IN 4",
                4 => "IN 5",
                5 => "IN 6",
                _ => "IN ?",
            },
            Column::Level => "LEVEL",
            Column::Compressor => "COMP",
            Column::MicGain => "MIC",
        }
    }
}

/// All columns: 6 inputs + level/comp/mic.
fn all_columns() -> Vec<Column> {
    let mut cols = Vec::with_capacity(9);
    for i in 0..NUM_INPUTS {
        cols.push(Column::Input(i));
    }
    cols.push(Column::Level);
    cols.push(Column::Compressor);
    cols.push(Column::MicGain);
    cols
}

const BUS_LABELS: [&str; 4] = [" A ", " B ", " C ", " D "];

struct TuiState {
    active_bus: usize,
    selected_col: usize,
}

pub async fn run(
    mut state_rx: watch::Receiver<MixerState>,
    cmd_tx: mpsc::Sender<Vec<Tuple>>,
) -> Result<()> {
    terminal::enable_raw_mode()?;
    let mut stdout = io::stdout();
    execute!(stdout, EnterAlternateScreen, cursor::Hide)?;
    let backend = CrosstermBackend::new(stdout);
    let mut terminal = Terminal::new(backend)?;

    let audio_meter = AudioMeter::try_start();
    let mut events = EventStream::new();
    let mut meter_tick = tokio::time::interval(std::time::Duration::from_millis(50));
    let has_meter = audio_meter.is_some();

    let mut tui = TuiState {
        active_bus: 0,
        selected_col: 0,
    };

    let cleanup = || {
        let _ = terminal::disable_raw_mode();
        let _ = execute!(io::stdout(), LeaveAlternateScreen, cursor::Show);
    };

    let mut needs_draw = true;

    loop {
        if needs_draw {
            let state = state_rx.borrow_and_update().clone();
            log::debug!(
                "draw: input_jack_sense=0b{:06b} output_jack_sense=0b{:04b}",
                state.input_jack_sense,
                state.output_jack_sense,
            );
            let cols = all_columns();

            if cols.is_empty() {
                tui.selected_col = 0;
            } else if tui.selected_col >= cols.len() {
                tui.selected_col = cols.len() - 1;
            }

            terminal.draw(|f| {
                let area = f.area();
                let chunks = Layout::default()
                    .direction(Direction::Vertical)
                    .constraints([
                        Constraint::Length(1),
                        Constraint::Length(2),
                        Constraint::Min(5),
                        Constraint::Length(2),
                    ])
                    .split(area);

                render_title(f, chunks[0], &state);
                render_tabs(f, chunks[1], &state, tui.active_bus);
                render_columns(f, chunks[2], &state, &cols, &tui, audio_meter.as_ref());
                render_status(f, chunks[3], &state, &tui);
            })?;
            needs_draw = false;
        }

        // Async select: wake on key event, BLE state change, or meter tick.
        tokio::select! {
            _ = meter_tick.tick(), if has_meter => {
                needs_draw = true;
                continue;
            }
            ev = events.next() => {
                let Some(Ok(Event::Key(key))) = ev else {
                    needs_draw = true;
                    continue;
                };
                if key.kind != KeyEventKind::Press {
                    continue;
                }
                needs_draw = true;

                let state = state_rx.borrow().clone();
                let cols = all_columns();
                let shift = key.modifiers.contains(KeyModifiers::SHIFT);

                macro_rules! send {
                    ($tuples:expr) => {{
                        let mut t: Vec<Tuple> = $tuples;
                        t.push(Tuple::new(ParamId::RequestState as u8, 0, 0));
                        let _ = cmd_tx.send(t).await;
                    }};
                }

                match key.code {
                    KeyCode::Char('q') | KeyCode::Esc => {
                        cleanup();
                        return Ok(());
                    }
                    KeyCode::Char('c') if key.modifiers.contains(KeyModifiers::CONTROL) => {
                        cleanup();
                        return Ok(());
                    }
                    KeyCode::Tab => {
                        tui.active_bus = (tui.active_bus + 1) % NUM_BUSES;
                    }
                    KeyCode::BackTab => {
                        tui.active_bus = if tui.active_bus == 0 {
                            NUM_BUSES - 1
                        } else {
                            tui.active_bus - 1
                        };
                    }
                    KeyCode::Left | KeyCode::Char('h') => {
                        if tui.selected_col > 0 {
                            tui.selected_col -= 1;
                        }
                    }
                    KeyCode::Right | KeyCode::Char('l') => {
                        if !cols.is_empty() && tui.selected_col < cols.len() - 1 {
                            tui.selected_col += 1;
                        }
                    }
                    KeyCode::Up | KeyCode::Char('k') => {
                        if let Some(&col) = cols.get(tui.selected_col) {
                            let param = col.param_id();
                            let bus = tui.active_bus as u8;
                            let step = if shift { 64 } else { 8 };
                            let new_val = state.get(param, bus).saturating_add(step);
                            send!(vec![Tuple::new(param as u8, bus, new_val)]);
                        }
                    }
                    KeyCode::Down | KeyCode::Char('j') => {
                        if let Some(&col) = cols.get(tui.selected_col) {
                            let param = col.param_id();
                            let bus = tui.active_bus as u8;
                            let step = if shift { 64 } else { 8 };
                            let new_val = state.get(param, bus).saturating_sub(step);
                            send!(vec![Tuple::new(param as u8, bus, new_val)]);
                        }
                    }
                    KeyCode::Char('m') => {
                        // Firmware treats mute as global boolean (0 = off, non-zero = on)
                        let new_mute = if state.mute == 0 { 0x0F } else { 0 };
                        send!(vec![Tuple::new(ParamId::MuteOutput as u8, 0, new_mute)]);
                    }
                    KeyCode::Char('c') => {
                        let new_comp = state.comp_on_off ^ (1 << (3 - tui.active_bus));
                        send!(vec![Tuple::new(ParamId::CompressorOnOff as u8, 0, new_comp)]);
                    }
                    KeyCode::Char('t') => {
                        let new_talk = u8::from(!state.talk);
                        send!(vec![Tuple::new(ParamId::Talk as u8, 0, new_talk)]);
                    }
                    _ => {}
                }
            }
            _ = state_rx.changed() => {
                needs_draw = true;
            }
        }
    }
}

fn render_title(f: &mut ratatui::Frame, area: Rect, state: &MixerState) {
    let connected = if state.connected {
        "CONNECTED"
    } else {
        "waiting..."
    };
    let title = Line::from(vec![
        Span::styled(
            "Blender Mixer  ",
            Style::default().add_modifier(Modifier::BOLD),
        ),
        Span::styled(
            connected,
            Style::default().fg(if state.connected {
                Color::Green
            } else {
                Color::DarkGray
            }),
        ),
    ]);
    f.render_widget(Paragraph::new(title), area);
}

fn render_tabs(f: &mut ratatui::Frame, area: Rect, state: &MixerState, active_bus: usize) {
    let titles: Vec<Line> = (0..NUM_BUSES)
        .map(|i| {
            let style = if !state.output_plugged(i) {
                Style::default().fg(Color::DarkGray)
            } else {
                Style::default()
            };
            Line::from(Span::styled(BUS_LABELS[i], style))
        })
        .collect();

    let tabs = Tabs::new(titles)
        .select(active_bus)
        .highlight_style(
            Style::default()
                .fg(Color::Cyan)
                .add_modifier(Modifier::BOLD),
        )
        .divider("│")
        .block(Block::default().borders(Borders::BOTTOM));

    f.render_widget(tabs, area);
}

/// Sub-cell vertical bar levels (index 0 = empty, 8 = full).
const BAR_LEVELS: [&str; 9] = [
    " ",
    bar::ONE_EIGHTH,
    bar::ONE_QUARTER,
    bar::THREE_EIGHTHS,
    bar::HALF,
    bar::FIVE_EIGHTHS,
    bar::THREE_QUARTERS,
    bar::SEVEN_EIGHTHS,
    bar::FULL,
];

/// Render a vertical bar (bottom-to-top) with sub-cell precision.
fn render_vbar(f: &mut ratatui::Frame, area: Rect, val: u8, color: Color) {
    let h = area.height as usize;
    let w = area.width as usize;
    if h == 0 || w == 0 {
        return;
    }
    // Total eighths of cell height filled
    let eighths = (val as usize) * h * 8 / 255;
    let full_rows = eighths / 8;
    let frac = eighths % 8;

    let mut lines: Vec<Line> = Vec::with_capacity(h);
    for row in 0..h {
        let from_bottom = h - 1 - row;
        if from_bottom < full_rows {
            lines.push(Line::from(Span::styled(
                BAR_LEVELS[8].repeat(w),
                Style::default().fg(color),
            )));
        } else if from_bottom == full_rows && frac > 0 {
            lines.push(Line::from(Span::styled(
                BAR_LEVELS[frac].repeat(w),
                Style::default().fg(color),
            )));
        } else {
            lines.push(Line::from(" ".repeat(w)));
        }
    }
    f.render_widget(Paragraph::new(lines), area);
}

fn render_columns(
    f: &mut ratatui::Frame,
    area: Rect,
    state: &MixerState,
    cols: &[Column],
    tui: &TuiState,
    audio_meter: Option<&AudioMeter>,
) {
    let col_constraints: Vec<Constraint> = cols
        .iter()
        .map(|_| Constraint::Ratio(1, cols.len() as u32))
        .collect();
    let col_areas = Layout::default()
        .direction(Direction::Horizontal)
        .constraints(col_constraints)
        .split(area);

    for (i, &col) in cols.iter().enumerate() {
        let is_selected = i == tui.selected_col;
        let param = col.param_id();
        let bus = tui.active_bus as u8;
        let val = state.get(param, bus);

        let border_color = if is_selected {
            Color::Cyan
        } else {
            Color::DarkGray
        };

        let muted = state.bus_muted(tui.active_bus);
        let unplugged = matches!(col, Column::Input(idx) if !state.input_plugged(idx));

        let block = Block::default()
            .title(col.label())
            .borders(Borders::ALL)
            .border_style(Style::default().fg(if unplugged {
                Color::DarkGray
            } else {
                border_color
            }));

        let inner = block.inner(col_areas[i]);
        f.render_widget(block, col_areas[i]);

        // Split inner: bar area + value label row
        let inner_chunks = Layout::default()
            .direction(Direction::Vertical)
            .constraints([Constraint::Min(1), Constraint::Length(1)])
            .split(inner);

        // Vertical bar (bottom-to-top)
        let bar_color = if unplugged {
            Color::DarkGray
        } else if muted {
            Color::Red
        } else if is_selected {
            Color::Cyan
        } else {
            Color::Blue
        };

        // Check if this column has a peak meter
        let peak_info = audio_meter.and_then(|meter| {
            if let Column::Input(idx) = col {
                Some(meter.input_peak(idx))
            } else {
                None
            }
        });

        if let Some(peak) = peak_info {
            // Split bar area: gain bar fills space, peak bar is 1 wide
            let bar_chunks = Layout::default()
                .direction(Direction::Horizontal)
                .constraints([Constraint::Min(1), Constraint::Length(1)])
                .split(inner_chunks[0]);
            render_vbar(f, bar_chunks[0], val, bar_color);

            let peak_color = match peak {
                0..=127 => Color::Green,
                128..=200 => Color::Yellow,
                201..=255 => Color::Red,
            };
            render_vbar(f, bar_chunks[1], peak, peak_color);
        } else {
            render_vbar(f, inner_chunks[0], val, bar_color);
        }

        // Value label
        let label = Span::styled(
            format!("{val:3}"),
            Style::default().fg(if unplugged {
                Color::DarkGray
            } else if is_selected {
                Color::White
            } else {
                Color::Gray
            }),
        );
        f.render_widget(Paragraph::new(Line::from(label)), inner_chunks[1]);
    }
}

fn render_status(f: &mut ratatui::Frame, area: Rect, state: &MixerState, tui: &TuiState) {
    let bus = tui.active_bus;
    let muted = state.bus_muted(bus);
    let comp = state.bus_comp_enabled(bus);

    let mute_style = if muted {
        Style::default().fg(Color::Red).add_modifier(Modifier::BOLD)
    } else {
        Style::default().fg(Color::DarkGray)
    };
    let comp_style = if comp {
        Style::default()
            .fg(Color::Yellow)
            .add_modifier(Modifier::BOLD)
    } else {
        Style::default().fg(Color::DarkGray)
    };
    let talk_style = if state.talk {
        Style::default()
            .fg(Color::Magenta)
            .add_modifier(Modifier::BOLD)
    } else {
        Style::default().fg(Color::DarkGray)
    };

    let status = Line::from(vec![
        Span::styled(
            format!("MUTE:{}", if muted { "ON " } else { "off" }),
            mute_style,
        ),
        Span::raw("  "),
        Span::styled(
            format!("COMP:{}", if comp { "ON " } else { "off" }),
            comp_style,
        ),
        Span::raw("  "),
        Span::styled(
            format!("TALK:{}", if state.talk { "ON " } else { "off" }),
            talk_style,
        ),
        Span::raw("  │  "),
        Span::styled(
            "Tab:bus h/l:ch j/k:val m:mute c:comp t:talk q:quit",
            Style::default().fg(Color::DarkGray),
        ),
    ]);

    f.render_widget(
        Paragraph::new(status).block(Block::default().borders(Borders::TOP)),
        area,
    );
}
