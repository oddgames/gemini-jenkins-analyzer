document.addEventListener('DOMContentLoaded', function () {
  if (
    window.location.pathname.match(/\/console(Full)?$/) &&
    !window.location.pathname.includes('/error-explanation')
  ) {
    checkBuildStatusAndAddButton();

    // Check if auto-analyze parameter is present
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('autoAnalyze') === 'true') {
      // Wait a moment for the page to fully load, then trigger analysis
      setTimeout(function() {
        analyzeConsoleError();
      }, 1000);
    }
  }

  // Move containers to correct position
  const logsContainer = document.getElementById('analyzer-error-logs-container');
  const container = document.getElementById('analyzer-error-container');
  const consoleOutput =
    document.querySelector('#out') ||
    document.querySelector('pre.console-output') ||
    document.querySelector('pre');

  if (logsContainer && consoleOutput && consoleOutput.parentNode) {
    try {
      if (logsContainer.parentNode !== consoleOutput.parentNode &&
          !consoleOutput.contains(logsContainer) &&
          !logsContainer.contains(consoleOutput)) {
        consoleOutput.parentNode.insertBefore(logsContainer, consoleOutput);
      }
    } catch (e) {
      console.warn('Could not move analyzer-error-logs-container:', e);
    }
  }

  if (container && consoleOutput && consoleOutput.parentNode) {
    try {
      // Only move if not already in correct position and not a descendant
      if (container.parentNode !== consoleOutput.parentNode &&
          !consoleOutput.contains(container) &&
          !container.contains(consoleOutput)) {
        consoleOutput.parentNode.insertBefore(container, consoleOutput);
      }
    } catch (e) {
      console.warn('Could not move analyzer-error-container:', e);
    }
  }

  // Move confirmation dialog to correct position
  const dialogContainer = document.getElementById('analyzer-error-confirm-dialog');
  if (dialogContainer && consoleOutput && consoleOutput.parentNode) {
    try {
      // Only move if not already in correct position and not a descendant
      if (dialogContainer.parentNode !== consoleOutput.parentNode &&
          !consoleOutput.contains(dialogContainer) &&
          !dialogContainer.contains(consoleOutput)) {
        consoleOutput.parentNode.insertBefore(dialogContainer, consoleOutput);
      }
    } catch (e) {
      console.warn('Could not move analyzer-error-confirm-dialog:', e);
    }
  }
});

function checkBuildStatusAndAddButton() {
  checkBuildStatus(function(buildingStatus) {
    // Build status 2 is completed and it's UNSTABLE or FAILURE
    if (buildingStatus == 2) {
      // Build is completed, show the button
      addAnalyzeErrorButton();
    } else if (buildingStatus == 1) {
      // Build is still running, check again after a delay
      setTimeout(checkBuildStatusAndAddButton, 5000); // Check every 5 seconds
    }
  });
}

function checkBuildStatus(callback) {
  const basePath = window.location.pathname.replace(/\/console(Full)?$/, '');
  const url = basePath + '/console-analyzer-error/checkBuildStatus';

  const headers = crumb.wrap({
    "Content-Type": "application/x-www-form-urlencoded",
  });

  fetch(url, {
    method: "POST",
    headers: headers,
    body: ""
  })
  .then(response => response.json())
  .then(data => {
    callback(data.buildingStatus);
  })
  .catch(error => {
    console.warn('Error checking build status:', error);
    // If check fails, assume build is complete and show button
    callback(false);
  });
}

function addAnalyzeErrorButton() {
  // Check if button already exists to prevent duplicates
  if (document.querySelector('.analyzer-error-btn')) {
    return;
  }

  // First try to find the existing console button bar
  const consoleButtonBar = 
    document.querySelector('.console-actions') ||
    document.querySelector('.console-output-actions') ||
    document.querySelector('.console-controls') ||
    document.querySelector('[class*="console"][class*="button"]') ||
    document.querySelector('#console .btn-group') ||
    document.querySelector('.jenkins-button-bar');

  // Try to find buttons by their text content
  let buttonContainer = null;
  const downloadButtons = Array.from(document.querySelectorAll('a, button')).filter(el => 
    el.textContent && (
      el.textContent.includes('Download') || 
      el.textContent.includes('Copy') || 
      el.textContent.includes('View as plain text')
    )
  );

  if (downloadButtons.length > 0) {
    buttonContainer = downloadButtons[0].parentElement;
  }

  // Fallback: find console output element
  const consoleOutput =
    document.querySelector('#out') ||
    document.querySelector('pre.console-output') ||
    document.querySelector('pre');

  if (!consoleOutput && !buttonContainer) {
    console.warn('Console output element not found');
    setTimeout(function() {
      // Only retry if the button doesn't exist yet and we're still on a console page
      if (!document.querySelector('.analyzer-error-btn') &&
          window.location.pathname.match(/\/console(Full)?$/)) {
        checkBuildStatusAndAddButton();
      }
    }, 3000);
    return;
  }

  const analyzeBtn = createButton('Analyze Error', 'jenkins-button analyzer-error-btn', analyzeConsoleError);

  // If we found the button container, add our button there
  if (buttonContainer) {
    buttonContainer.insertBefore(analyzeBtn, buttonContainer.firstChild);
  } else if (consoleButtonBar) {
    consoleButtonBar.appendChild(analyzeBtn);
  } else {
    // Fallback: create a simple container above console output
    const container = document.createElement('div');
    container.className = 'analyzer-error-container';
    container.style.marginBottom = '10px';
    container.appendChild(analyzeBtn);
    consoleOutput.parentNode.insertBefore(container, consoleOutput);
  }
}

function createButton(text, className, onClick) {
  const btn = document.createElement('button');
  btn.textContent = text;
  btn.className = className;
  btn.onclick = onClick;
  return btn;
}

function analyzeConsoleError() {
  // First, check if an analysis already exists
  checkExistingAnalysis();
}

function checkExistingAnalysis() {
  const basePath = window.location.pathname.replace(/\/console(Full)?$/, '');
  const url = basePath + '/console-analyzer-error/checkExistingAnalysis';

  const headers = crumb.wrap({
    "Content-Type": "application/x-www-form-urlencoded",
  });

  fetch(url, {
    method: "POST",
    headers: headers,
    body: ""
  })
  .then(response => response.json())
  .then(data => {
    if (data.hasAnalysis) {
      // Show confirmation dialog
      showConfirmationDialog(data.timestamp);
    } else {
      // No existing analysis, proceed with new request
      sendAnalyzeRequest(false);
    }
  })
  .catch(error => {
    console.warn('Error checking existing analysis:', error);
    // If check fails, proceed with new request
    sendAnalyzeRequest(false);
  });
}

function showConfirmationDialog(timestamp) {
  const dialog = document.getElementById('analyzer-error-confirm-dialog');
  const timestampSpan = document.getElementById('existing-analysis-timestamp');
  
  if (timestampSpan) {
    timestampSpan.textContent = timestamp;
  }
  
  dialog.classList.remove('jenkins-hidden');
  
  // Hide other elements
  hideContainer();
}

Behaviour.specify(".eep-view-existing-button", "AnalyzerErrorView", 0, function(e) {
  e.onclick = viewExistingAnalysis;
});


function viewExistingAnalysis() {
  hideConfirmationDialog();
  sendAnalyzeRequest(false); // This will return the cached result
}

Behaviour.specify(".eep-generate-new-button", "AnalyzerErrorView", 0, function(e) {
  e.onclick = generateNewAnalysis;
});

function generateNewAnalysis() {
  hideConfirmationDialog();
  clearAnalysisContent();

  // Show immediate feedback that new analysis is starting
  const statusIcon = document.getElementById('analyzer-status-icon');
  const statusText = document.getElementById('analyzer-status-text');
  if (statusIcon) statusIcon.textContent = '🔄';
  if (statusText) statusText.textContent = 'Regenerating AI Analysis...';

  sendAnalyzeRequest(true); // Force new analysis
}

Behaviour.specify(".eep-cancel-button", "AnalyzerErrorView", 0, function(e) {
  e.onclick = cancelAnalysis;
});

function cancelAnalysis() {
  hideConfirmationDialog();
}

function hideConfirmationDialog() {
  const dialog = document.getElementById('analyzer-error-confirm-dialog');
  dialog.classList.add('jenkins-hidden');
}

function sendAnalyzeRequest(forceNew = false) {
  const basePath = window.location.pathname.replace(/\/console(Full)?$/, '');
  const analyzeUrl = basePath + '/console-analyzer-error/explainConsoleError';
  const logsUrl = basePath + '/console-analyzer-error/getFilteredLogs';

  const headers = crumb.wrap({
    "Content-Type": "application/x-www-form-urlencoded",
  });

  // Add forceNew parameter if needed
  const body = forceNew ? "forceNew=true" : "";

  showSpinner();

  // First, fetch and display the filtered logs
  fetch(logsUrl, {
    method: "POST",
    headers: headers,
    body: ""
  })
  .then(response => response.text())
  .then(responseText => {
    try {
      const filteredLogs = JSON.parse(responseText);
      if (filteredLogs && filteredLogs.trim()) {
        showErrorLogs(filteredLogs);
      }
    } catch (e) {
      console.warn('Could not parse filtered logs:', e);
    }
  })
  .catch(error => {
    console.warn('Could not fetch filtered logs:', error);
  });

  // Then send the analysis request
  fetch(analyzeUrl, {
    method: "POST",
    headers: headers,
    body: body
  })
  .then(response => {
    if (!response.ok) {
      notificationBar.show('Analysis failed', notificationBar.ERROR);
    }
    return response.text();
  })
  .then(responseText => {
    try {
      const jsonResponse = JSON.parse(responseText);
      showErrorAnalysis(jsonResponse);
    } catch (e) {
      showErrorAnalysis(responseText);
    }
  })
  .catch(error => {
    showErrorAnalysis(`Error: ${error.message}`);
  });
}

function showErrorAnalysis(message) {
  const container = document.getElementById('analyzer-error-container');
  const spinner = document.getElementById('analyzer-error-spinner');
  const content = document.getElementById('analyzer-error-content');
  const statusIcon = document.getElementById('analyzer-status-icon');
  const statusText = document.getElementById('analyzer-status-text');

  container.classList.remove('jenkins-hidden');
  spinner.classList.add('jenkins-hidden');
  content.textContent = message;

  // Update status to show completion
  if (statusIcon) statusIcon.textContent = '✅';
  if (statusText) statusText.textContent = 'AI Error Analysis (Complete)';
}

function showSpinner() {
  const container = document.getElementById('analyzer-error-container');
  const spinner = document.getElementById('analyzer-error-spinner');
  const content = document.getElementById('analyzer-error-content');
  const statusIcon = document.getElementById('analyzer-status-icon');
  const statusText = document.getElementById('analyzer-status-text');
  const details = document.getElementById('analyzer-error-details');

  container.classList.remove('jenkins-hidden');
  spinner.classList.remove('jenkins-hidden');

  // Clear previous content
  if (content) content.textContent = '';

  // Update status to show it's generating
  if (statusIcon) statusIcon.textContent = '⚡';
  if (statusText) statusText.textContent = 'Generating AI Analysis...';

  // Ensure details is open so user sees the spinner
  if (details) details.setAttribute('open', 'true');
}

function hideContainer() {
  const container = document.getElementById('analyzer-error-container');
  container.classList.add('jenkins-hidden');
}

function clearAnalysisContent() {
  const content = document.getElementById('analyzer-error-content');
  if (content) {
    content.textContent = '';
  }
  hideErrorLogsContainer();
  const logsContent = document.getElementById('analyzer-error-logs-content');
  if (logsContent) {
    logsContent.textContent = '';
  }
}

function showErrorLogs(logs) {
  const logsContainer = document.getElementById('analyzer-error-logs-container');
  const logsContent = document.getElementById('analyzer-error-logs-content');

  if (logsContainer && logsContent) {
    logsContainer.classList.remove('jenkins-hidden');
    logsContent.textContent = logs;
  }
}

function hideErrorLogsContainer() {
  const logsContainer = document.getElementById('analyzer-error-logs-container');
  if (logsContainer) {
    logsContainer.classList.add('jenkins-hidden');
  }
}

function copyErrorLogsToClipboard(event) {
  event.stopPropagation(); // Prevent details from toggling
  const logsContent = document.getElementById('analyzer-error-logs-content');
  if (logsContent && logsContent.textContent) {
    copyToClipboard(logsContent.textContent, event.target);
  }
}

function copyAnalysisToClipboard(event) {
  event.stopPropagation(); // Prevent details from toggling
  const analysisContent = document.getElementById('analyzer-error-content');
  if (analysisContent && analysisContent.textContent) {
    copyToClipboard(analysisContent.textContent, event.target);
  }
}

function copyToClipboard(text, buttonElement) {
  if (!text) {
    notificationBar.show('Nothing to copy', notificationBar.WARNING);
    return;
  }

  // Find the button element (could be the svg or button itself)
  const button = buttonElement.closest('.analyzer-copy-btn');

  // Modern clipboard API
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text)
      .then(() => {
        showCopySuccess(button);
      })
      .catch(err => {
        console.error('Failed to copy:', err);
        fallbackCopyToClipboard(text, button);
      });
  } else {
    // Fallback for older browsers
    fallbackCopyToClipboard(text, button);
  }
}

function fallbackCopyToClipboard(text, button) {
  const textArea = document.createElement('textarea');
  textArea.value = text;
  textArea.style.position = 'fixed';
  textArea.style.left = '-999999px';
  textArea.style.top = '-999999px';
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();

  try {
    const successful = document.execCommand('copy');
    if (successful) {
      showCopySuccess(button);
    } else {
      notificationBar.show('Failed to copy to clipboard', notificationBar.ERROR);
    }
  } catch (err) {
    console.error('Failed to copy:', err);
    notificationBar.show('Failed to copy to clipboard', notificationBar.ERROR);
  }

  document.body.removeChild(textArea);
}

function showCopySuccess(button) {
  if (button) {
    const originalText = button.innerHTML;
    button.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg> Copied!';
    button.style.color = 'var(--success-color, #28a745)';

    setTimeout(() => {
      button.innerHTML = originalText;
      button.style.color = '';
    }, 2000);
  }

  // Also show notification
  notificationBar.show('Copied to clipboard', notificationBar.SUCCESS);
}
