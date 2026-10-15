window.createAgentPdf = function(text, filename) {
  if (!window.AndroidPDF) {
    window.showAgentMessage("PDF save feature is not connected yet.");
    return;
  }
  window.AndroidPDF.saveTextAsPdf(String(text || ""), String(filename || "AI-Agent.pdf"));
};
