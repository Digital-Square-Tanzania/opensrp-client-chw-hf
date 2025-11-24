function loadData(reportKey, reportType) {
  const rawData = Android.getData(reportKey);
  if (!rawData) {
    return;
  }

  let data;
  try {
    data = JSON.parse(rawData);
  } catch (error) {
    return;
  }
  const nameValuePairs = data && data.nameValuePairs ? data.nameValuePairs : {};
  const keys = Object.keys(nameValuePairs);
  const reportPeriod = document.getElementById("report_period");
  const reportingFacility = document.getElementById("reporting_facility");
  keys.forEach((key) => {
    let element;
    if (reportType !== null && reportType === "pnc") {
      element = document.getElementById(key.replace("pnc-", ""));
    } else {
      element = document.getElementById(key);
    }
    if (element !== null && typeof element !== "undefined") {
      element.innerHTML = nameValuePairs[key];
    }
  });

  const tableBody = document.getElementById("table-body");
  const hasReportData = nameValuePairs.reportData && Array.isArray(nameValuePairs.reportData.values);
  if (tableBody !== null && hasReportData) {
    nameValuePairs.reportData.values.forEach((dataPoint) => {
      if (!dataPoint || !dataPoint.nameValuePairs) {
        return;
      }
      const row = document.createElement("tr");
      const dataPointKeys = Object.keys(dataPoint.nameValuePairs);
      dataPointKeys.forEach((key) => {
        const cell = document.createElement("td");
        cell.innerHTML = dataPoint.nameValuePairs[key];
        row.appendChild(cell);
      });
      tableBody.appendChild(row);
    });
  }

  if (reportPeriod !== null && typeof reportPeriod !== "undefined") {
    reportPeriod.innerHTML = Android.getDataPeriod();
  }
  if (reportingFacility !== null && typeof reportingFacility !== "undefined") {
    reportingFacility.innerHTML = Android.getReportingChw();
  }
}
