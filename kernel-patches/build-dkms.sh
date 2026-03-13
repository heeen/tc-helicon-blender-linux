#!/bin/bash
#
# Build and install a DKMS module for TC Helicon USB audio support.
#
# Copies the upstream snd-usb-audio source from the running kernel,
# applies our patches, and installs via DKMS.
#
# Usage: sudo ./build-dkms.sh [--remove]
#
set -euo pipefail

MODULE_NAME="snd-usb-audio-tc-helicon"
MODULE_VERSION="0.2"
DKMS_SRC="/usr/src/${MODULE_NAME}-${MODULE_VERSION}"
PATCH_DIR="$(cd "$(dirname "$0")" && pwd)"
KERNEL_VERSION="$(uname -r)"
KERNEL_SRC="/usr/src/linux-headers-${KERNEL_VERSION}"

# Find upstream snd-usb-audio source
if [ -d "/lib/modules/${KERNEL_VERSION}/build/sound/usb" ]; then
    USB_SRC="/lib/modules/${KERNEL_VERSION}/build/sound/usb"
elif [ -d "${KERNEL_SRC}/sound/usb" ]; then
    USB_SRC="${KERNEL_SRC}/sound/usb"
else
    echo "Error: Cannot find kernel source for ${KERNEL_VERSION}" >&2
    echo "Install linux-headers-${KERNEL_VERSION}" >&2
    exit 1
fi

if [ "${1:-}" = "--remove" ]; then
    echo "Removing DKMS module ${MODULE_NAME}/${MODULE_VERSION}..."
    dkms remove "${MODULE_NAME}/${MODULE_VERSION}" --all 2>/dev/null || true
    rm -rf "${DKMS_SRC}"
    echo "Done."
    exit 0
fi

echo "=== TC Helicon snd-usb-audio DKMS builder ==="
echo "Kernel:  ${KERNEL_VERSION}"
echo "Source:  ${USB_SRC}"
echo "Patches: ${PATCH_DIR}/000*.patch"
echo ""

# Remove old DKMS module if present
dkms status "${MODULE_NAME}" 2>/dev/null | grep -q "${MODULE_VERSION}" && \
    dkms remove "${MODULE_NAME}/${MODULE_VERSION}" --all 2>/dev/null || true
rm -rf "${DKMS_SRC}"

# Copy upstream source
echo "Copying upstream snd-usb-audio source..."
mkdir -p "${DKMS_SRC}"
cp "${USB_SRC}"/*.c "${USB_SRC}"/*.h "${DKMS_SRC}/"

# Generate Makefile (out-of-tree module build)
cat > "${DKMS_SRC}/Makefile" << 'MAKEFILE'
snd-usb-audio-y := card.o clock.o endpoint.o format.o helper.o implicit.o \
	mixer.o mixer_maps.o mixer_quirks.o mixer_scarlett.o mixer_scarlett2.o \
	mixer_s1810c.o mixer_us16x08.o pcm.o power.o proc.o quirks.o stream.o \
	validate.o media.o midi.o midi2.o

obj-m := snd-usb-audio.o
MAKEFILE

# Generate DKMS config
cat > "${DKMS_SRC}/dkms.conf" << DKMSCONF
PACKAGE_NAME="${MODULE_NAME}"
PACKAGE_VERSION="${MODULE_VERSION}"
BUILT_MODULE_NAME[0]="snd-usb-audio"
DEST_MODULE_LOCATION[0]="/updates"
AUTOINSTALL="yes"
DKMSCONF

# Apply patches
echo "Applying patches..."
for patch in "${PATCH_DIR}"/000*.patch; do
    name="$(basename "$patch")"
    echo "  ${name}"
    # Patches are rooted at sound/usb/, strip 3 path components
    patch -d "${DKMS_SRC}" -p3 < "$patch"
done

# Build and install
echo ""
echo "Running DKMS add + build + install..."
dkms add "${MODULE_NAME}/${MODULE_VERSION}"
dkms build "${MODULE_NAME}/${MODULE_VERSION}"
dkms install "${MODULE_NAME}/${MODULE_VERSION}"

echo ""
echo "Done! Module installed. To activate without reboot:"
echo "  sudo modprobe -r snd_usb_audio && sudo modprobe snd_usb_audio"
echo ""
echo "To remove:"
echo "  sudo $0 --remove"
