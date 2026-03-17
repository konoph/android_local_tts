#!/bin/bash

# Directory to store the assets
ASSET_DIR="app/src/main/assets/kokoro"
mkdir -p "$ASSET_DIR"

echo "Downloading Kokoro-82M Japanese multi-lang model for Sherpa-ONNX..."

# Base URL for Sherpa-ONNX TTS models
MODEL_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2"
TMP_FILE="kokoro_model.tar.bz2"

# Download the model
if [ ! -f "$TMP_FILE" ]; then
    wget "$MODEL_URL" -O "$TMP_FILE"
fi

# Extract relevant files
echo "Extracting model files..."
tar xvf "$TMP_FILE"

# Copy essential files to the assets directory
cp kokoro-multi-lang-v1_0/model.onnx "$ASSET_DIR/"
cp kokoro-multi-lang-v1_0/voices.bin "$ASSET_DIR/"
cp kokoro-multi-lang-v1_0/tokens.txt "$ASSET_DIR/"
cp -r kokoro-multi-lang-v1_0/espeak-ng-data "$ASSET_DIR/"

# Cleanup
rm -rf kokoro-multi-lang-v1_0
# Keep the tar.bz2 if you want to avoid re-downloading
# rm "$TMP_FILE"

echo "Setup complete. Model files are in $ASSET_DIR"
