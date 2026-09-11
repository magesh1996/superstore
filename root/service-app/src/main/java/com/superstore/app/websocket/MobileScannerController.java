package com.superstore.app.websocket;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class MobileScannerController {

    @GetMapping(value = "/scan/{sessionId}", produces = MediaType.TEXT_HTML_VALUE)
    public String mobileScanPage(@PathVariable String sessionId, HttpServletRequest request) {
        String host = request.getServerName() + ":" + request.getServerPort();
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Barcode Scanner</title>
              <script src="https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js"></script>
              <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body { font-family: sans-serif; background: #1a1a2e; color: white; 
                       display: flex; flex-direction: column; align-items: center; 
                       min-height: 100vh; padding: 20px; }
                h2  { margin: 16px 0 8px; font-size: 1.2em; color: #e0e0e0; }
                #reader { width: 100%%; max-width: 380px; border-radius: 12px; overflow: hidden; }
                #status { margin: 12px 0; padding: 10px 20px; border-radius: 8px;
                          background: #16213e; font-size: 0.95em; text-align: center;
                          min-width: 200px; }
                #result-box { margin-top: 10px; padding: 14px 20px; border-radius: 10px;
                              background: #0f3460; font-size: 1.3em; font-weight: bold;
                              text-align: center; display: none; min-width: 200px; }
                .success { background: #1a5c38 !important; color: #7aff9e; }
                .error   { background: #5c1a1a !important; color: #ff7a7a; }
              </style>
            </head>
            <body>
              <h2>📦 scan product barcode</h2>
              <div id="reader"></div>
              <div id="status">🔌 connecting...</div>
              <div id="result-box"></div>

              <script>
                const SESSION_ID = '%s';
                const WS_URL    = 'ws://%s/ws/barcode/' + SESSION_ID + '?role=mobile';

                let ws;
                let lastSent = '';
                let lastSentTime = 0;
                const DEBOUNCE_MS = 2000;

                function connectWs() {
                  ws = new WebSocket(WS_URL);

                  ws.onopen = () => {
                    document.getElementById('status').textContent = '✅ connected, point at barcode.';
                  };

                  ws.onerror = () => {
                    document.getElementById('status').textContent = '❌ WS error. retrying...';
                    setTimeout(connectWs, 2000);
                  };

                  ws.onclose = () => {
                    document.getElementById('status').textContent = '🔄 reconnecting...';
                    setTimeout(connectWs, 2000);
                  };
                }

                function sendBarcode(code) {
                  const now = Date.now();
                  // debounce : skip if same code sent recently
                  if (code === lastSent && (now - lastSentTime) < DEBOUNCE_MS) return;
                  lastSent     = code;
                  lastSentTime = now;

                  const resultBox = document.getElementById('result-box');
                  resultBox.style.display = 'block';

                  if (ws && ws.readyState === WebSocket.OPEN) {
                    ws.send(code);
                    resultBox.className = 'success';
                    resultBox.textContent = '✅ sent : [' + code + ']';
                    document.getElementById('status').textContent = 'scan another or close tab.';
                  } else {
                    resultBox.className = 'error';
                    resultBox.textContent = '❌ not connected, try again.';
                  }
                }

                // start scanner
                const scanner = new Html5QrcodeScanner('reader', {
                  fps: 10,
                  qrbox: { width: 280, height: 180 },
                  supportedScanTypes: [
                    Html5QrcodeScanType.SCAN_TYPE_CAMERA
                  ],
                  formatsToSupport: [
                    Html5QrcodeSupportedFormats.EAN_13,
                    Html5QrcodeSupportedFormats.EAN_8,
                    Html5QrcodeSupportedFormats.CODE_128,
                    Html5QrcodeSupportedFormats.CODE_39,
                    Html5QrcodeSupportedFormats.UPC_A,
                    Html5QrcodeSupportedFormats.UPC_E,
                    Html5QrcodeSupportedFormats.QR_CODE
                  ]
                }, false);

                scanner.render(
                  (decodedText, decodedResult) => { sendBarcode(decodedText); },
                  (errorMsg) => { /* ignore per-frame errors */ }
                );

                connectWs();
              </script>
            </body>
            </html>
            """.formatted(sessionId, host);
    }
}