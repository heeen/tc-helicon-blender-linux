-- TC Helicon Blender — dynamic stereo loopbacks
--
-- Creates 6 stereo virtual sources from the Blender's 12-channel capture
-- when the device is present, and destroys them on unplug.
--
-- Uses ObjectManager to watch for the Blender's multichannel input node.
-- LocalModule lifetime is tied to the variable reference: setting it to nil
-- + forcing collectgarbage() unloads the module immediately.
--
-- WirePlumber 0.5 + PipeWire 1.x

-- Must be module-level (not local) to avoid garbage collection.
blender_loopbacks = {}

local channel_map = {
  { "AUX0",  "AUX1"  },
  { "AUX2",  "AUX3"  },
  { "AUX4",  "AUX5"  },
  { "AUX6",  "AUX7"  },
  { "AUX8",  "AUX9"  },
  { "AUX10", "AUX11" },
}

local function make_stereo_source(index, aux_l, aux_r, hw_node_name)
  local name         = string.format("blender_input_%d", index)
  local desc         = string.format("Blender Input %d", index)
  local capture_name = string.format("blender_capture_%d", index)
  -- The capture side targets the physical 12ch node.
  -- node.passive: don't keep the hardware running on its own.
  -- node.dont-reconnect: do NOT fall back to another source (e.g. webcam)
  --   when the Blender disconnects.
  local args = string.format([[
    audio.position = [ FL FR ]
    capture.props = {
      node.name            = "%s"
      audio.position       = [ %s %s ]
      stream.dont-remix    = true
      node.passive         = true
      node.dont-reconnect  = true
      target.object        = "%s"
    }
    playback.props = {
      node.name            = "%s"
      media.class          = "Audio/Source"
      audio.position       = [ FL FR ]
      node.description     = "%s"
    }
  ]], capture_name, aux_l, aux_r, hw_node_name, name, desc)
  return LocalModule("libpipewire-module-loopback", args)
end

local function destroy_loopbacks()
  local count = #blender_loopbacks
  if count == 0 then
    return
  end
  for i = 1, count do
    blender_loopbacks[i] = nil
  end
  blender_loopbacks = {}
  -- Force immediate cleanup — don't wait for GC to unload the modules.
  collectgarbage("collect")
  Log.info("blender-loopbacks: destroyed " .. count .. " stereo loopback sources")
end

-- Watch for the Blender multichannel capture node.
--
-- node.name for the 12-ch input is:
--   alsa_input.usb-TC-Helicon_Blender_<serial>-00.multichannel-input
-- We match the prefix+suffix with a glob; serial varies per unit.
-- media.class = Audio/Source ensures we only fire on the source node,
-- not any sink or loopback node we created ourselves.
--
-- ObjectManager MUST be module-level, not local — local vars are GC'd
-- as soon as the script's top-level scope returns.
blender_om = ObjectManager {
  Interest {
    type = "node",
    Constraint { "media.class", "equals",  "Audio/Source",                                         type = "pw-global" },
    Constraint { "node.name",   "matches", "alsa_input.usb-TC-Helicon_Blender_*.multichannel-input", type = "pw-global" }
  }
}

blender_om:connect("object-added", function(om, node)
  local nname = node.properties["node.name"] or "(unknown)"
  Log.info("blender-loopbacks: multichannel node appeared: " .. nname)

  if #blender_loopbacks > 0 then
    Log.info("blender-loopbacks: loopbacks already exist, destroying stale ones first")
    destroy_loopbacks()
  end

  for i, pair in ipairs(channel_map) do
    blender_loopbacks[i] = make_stereo_source(i, pair[1], pair[2], nname)
  end
  Log.info("blender-loopbacks: created 6 stereo loopback sources targeting " .. nname)
end)

blender_om:connect("object-removed", function(om, node)
  local nname = node.properties["node.name"] or "(unknown)"
  Log.info("blender-loopbacks: multichannel node removed: " .. nname)
  destroy_loopbacks()
end)

blender_om:activate()
Log.info("blender-loopbacks: ObjectManager activated, watching for TC-Helicon Blender multichannel node")
