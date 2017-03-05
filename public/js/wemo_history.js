'use strict';

var WemoHistory = (function() {

  var canvas, container, context;
  var initialized = false;

  var height = 0;
  var width = 0;

  var divSize = 10;

  var binTextSize = 15;

  var labelTextSize = 30;
  var xLeftMargin = 25;
  var xAxisPadding = 25;
  var yTopMargin = 25;
  var yAxisPadding = 25;
  var labelMargin = 10;
  var yGranularity = 6;
  var axisWidth = 2;

  var barMargin = 6;

  var locale = 'en-US';

  var data = null;

  function init() {
    container = document.getElementById('graph_container');
    canvas = document.getElementById('graph');
    context = canvas.getContext('2d');
    window.addEventListener('resize', resizeCanvas, false);
  }

  function resizeCanvas() {
    canvas.width = container.clientWidth;
    canvas.height = container.clientHeight;
    width = canvas.width;
    height = canvas.height;
    if (data) renderAxis(data);
  }

  function load(rawData) {
    data = prepareData(rawData);
    resizeCanvas();
    renderAxis(data);
  }

  function prepareData(data) {
    var date = new Date(data[0].timestamp);
    var month = date.toLocaleString(locale, { month: "long" });

    var graphData = {
      month: month,
      energyTodayWh: {
        min: Number.MAX_VALUE,
        max: 0,
        values: [],
        unitExp: 1
      }
    };

    data.forEach(function (slot) {
      if (slot.usage) {
        graphData.energyTodayWh.max = Math.max(graphData.energyTodayWh.max, slot.usage.energyTodayWh);
        graphData.energyTodayWh.min = Math.min(graphData.energyTodayWh.min, slot.usage.energyTodayWh);
        graphData.energyTodayWh.values.push(slot.usage.energyTodayWh * 1000);
      } else {
        graphData.energyTodayWh.values.push(0);
      }
    });
    graphData.energyTodayWh.max = graphData.energyTodayWh.max * 1000;
    graphData.energyTodayWh.min = graphData.energyTodayWh.min * 1000;

    if (graphData.energyTodayWh.max == 0) {
      graphData.energyTodayWh.divIncr = 200;
      graphData.energyTodayWh.granularity = 5;
      graphData.energyTodayWh.maxDiv = 1000;
    } else {
      var maxRounded = Math.ceil(graphData.energyTodayWh.max / 100) * 100;
      graphData.energyTodayWh.divIncr = Math.ceil((maxRounded / yGranularity) / 100) * 100;
      graphData.energyTodayWh.granularity = Math.ceil(graphData.energyTodayWh.max / graphData.energyTodayWh.divIncr);
      graphData.energyTodayWh.maxDiv = graphData.energyTodayWh.divIncr * graphData.energyTodayWh.granularity;
    }

    return graphData;
  }

  function renderAxis(data) {

    var xAreaHeight = divSize + binTextSize + labelTextSize;

    var yArea = {
      x: 1,
      y: 1 + yTopMargin,
      width: divSize + binTextSize * 3 + labelMargin + labelTextSize * 2,
      height: height - yTopMargin - xAreaHeight - 1
    };

    var graph = {
      x: yArea.width + 1,
      y: yArea.y,
      width: width - yArea.width - xLeftMargin - axisWidth,
      height: yArea.height - divSize
    };

    // Axis lines
    context.save();
    context.lineWidth = axisWidth;
    context.beginPath();
    context.moveTo(graph.x, graph.y);
    context.lineTo(graph.x, graph.height);
    context.lineTo(graph.x + graph.width, graph.height);
    context.stroke();
    context.restore();

    // Y divs
    context.beginPath();
    context.save();
    context.moveTo(graph.x, graph.height);
    var binWidth = Math.floor((graph.height - graph.y - yAxisPadding) / data.energyTodayWh.granularity);
    for (var index = 1; index <= data.energyTodayWh.granularity; index++) {
      var yPos = graph.height - binWidth * index;

      context.moveTo(graph.x, yPos - axisWidth - 0.5);
      context.lineTo(graph.x - divSize, yPos - axisWidth - 0.5);
      context.lineTo(graph.x + graph.width, yPos - axisWidth - 0.5);

      renderText(index * data.energyTodayWh.divIncr, graph.x - divSize - labelMargin, yPos - axisWidth, 'black', binTextSize, false);
    }
    context.restore();

    // X divs
    context.save();
    context.moveTo(graph.x, graph.height);
    binWidth = Math.floor((graph.width - xAxisPadding) / data.energyTodayWh.values.length);
    data.energyTodayWh.values.forEach(function (slot, pos) {
      var index = pos + 1;
      var xPos = graph.x + binWidth * index + axisWidth / 2;
      context.moveTo(xPos - 0.5, graph.height);
      context.lineTo(xPos - 0.5, graph.height + divSize);
      renderText(index, xPos, graph.height + divSize , 'black', binTextSize, true);
    });
    context.strokeStyle = 'rgb(0, 0, 0)';
    context.lineWidth = 1;
    context.restore();

    context.save();
    renderVerticalText('Energy (mWH)', yArea.x + labelTextSize / 2, yArea.height / 2, 'black', labelTextSize);
    renderText(data.month, graph.x + graph.width / 2, graph.height + divSize + binTextSize + labelMargin, 'black', labelTextSize, true);
    context.restore();

    context.stroke();
    renderEnergyTodayWh(data.energyTodayWh, graph, binWidth);
  }

  function renderText(text, x, y, color, size, horizontalAlign) {
    context.font = size + 'px Comic Sans MS';
    context.fillStyle = color;
    if (horizontalAlign) {
      context.textAlign = 'center';
      context.fillText(text, x, y + size);
    } else {
      context.textBaseline = 'middle';
      var metrics = context.measureText(text);
      context.fillText(text, x - metrics.width, y );
    }
  }

  function renderVerticalText(text, x, y, color, size) {
    context.font = size + 'px Comic Sans MS';
    context.fillStyle = color;

    context.save();
    context.translate(x, y);
    context.rotate(-Math.PI / 2);
    context.textAlign = "center";
    context.fillText(text, 0, size);
    context.restore();
  }

  function renderEnergyTodayWh(energyTodayWh, graph, binWidth) {
    var barWidth = binWidth - barMargin;
    var barX = Math.floor(barWidth / 2);

    context.save();
    context.strokeStyle = 'rgb(0, 0, 179)';
    context.fillStyle = 'rgb(0, 0, 255)';
    context.lineWidth = 4;

    var graphHeight = graph.y + axisWidth + yAxisPadding - graph.height;
    var graphBaseLine = graph.height - axisWidth / 2;

    energyTodayWh.values.forEach(function (value, pos) {
      var index = pos + 1;
      var xPos = graph.x + binWidth * index;
      var yValue = Math.floor((graphHeight) * (value / energyTodayWh.maxDiv));
      context.fillRect(xPos - barX, graphBaseLine, barWidth, yValue);
    });
    context.restore();
  }

  return {
    init: init,
    load: load
  };
}());