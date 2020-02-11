define(["lodash","app/plugins/sdk","jquery"],function(e,t,n){return function(e){var t={};function n(o){if(t[o])return t[o].exports;var r=t[o]={i:o,l:!1,exports:{}};return e[o].call(r.exports,r,r.exports,n),r.l=!0,r.exports}return n.m=e,n.c=t,n.d=function(e,t,o){n.o(e,t)||Object.defineProperty(e,t,{enumerable:!0,get:o})},n.r=function(e){"undefined"!=typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(e,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(e,"__esModule",{value:!0})},n.t=function(e,t){if(1&t&&(e=n(e)),8&t)return e;if(4&t&&"object"==typeof e&&e&&e.__esModule)return e;var o=Object.create(null);if(n.r(o),Object.defineProperty(o,"default",{enumerable:!0,value:e}),2&t&&"string"!=typeof e)for(var r in e)n.d(o,r,function(t){return e[t]}.bind(null,r));return o},n.n=function(e){var t=e&&e.__esModule?function(){return e.default}:function(){return e};return n.d(t,"a",t),t},n.o=function(e,t){return Object.prototype.hasOwnProperty.call(e,t)},n.p="",n(n.s=2)}([function(t,n){t.exports=e},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var o=function(){function e(){}return e.defaultTrace={mapping:{x:null,y:null,z:null,text:null,color:null,size:null},show:{line:!0,markers:!0},settings:{line:{color:"#005f81",width:6,dash:"solid",shape:"linear"},marker:{size:15,symbol:"circle",color:"#33B5E5",colorscale:"YlOrRd",sizemode:"diameter",sizemin:3,sizeref:.2,line:{color:"#DDD",width:0},showscale:!1},color_option:"ramp"}},e.defaultQueryDescription={columnNames:{dataColumn:"",xColumn:"",yColumn:"",lonColumn:"",latColumn:""},queryTitle:"",queryNumber:0,color:"gray",yaxistext:""},e.defaultConfig={pconfig:{loadFromCDN:!1,showAnnotations:!0,fixScale:"",traces:[e.defaultTrace],settings:{type:"bar",fill:"None",mode:"None",displayModeBar:!0},layout:{showlegend:!1,legend:{orientation:"h"},barmode:"stack",dragmode:"zoom",font:{family:'"Open Sans", Helvetica, Arial, sans-serif'},xaxis:{autorange:!0,fixedrange:!1,showgrid:!0,zeroline:!1,type:"auto",rangemode:"normal"},yaxis:{autorange:!0,fixedrange:!1,showgrid:!0,zeroline:!1,type:"linear",rangemode:"normal"}}}},e}();t.defaultValues=o},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),t.PanelCtrl=t.PlotlyPanelCtrl=void 0;var o=n(3),r=h(n(0)),i=h(n(4)),a=n(5),s=n(6),l=n(7),u=n(9),c=n(10),p=n(1);function h(e){return e&&e.__esModule?e:{default:e}}var d,f,g=(d=function(e,t){return(d=Object.setPrototypeOf||{__proto__:[]}instanceof Array&&function(e,t){e.__proto__=t}||function(e,t){for(var n in t)t.hasOwnProperty(n)&&(e[n]=t[n])})(e,t)},function(e,t){function n(){this.constructor=e}d(e,t),e.prototype=null===t?Object.create(t):(n.prototype=t.prototype,new n)}),y=function(e){function t(t,n,o,i,a){var s=e.call(this,t,n)||this;return s.uiSegmentSrv=i,s.annotationsSrv=a,s.debug=!1,s.defaultPanelConfigs=p.defaultValues.defaultConfig,s.dataList=[],s.pointsSelected=void 0,s.annotations=new u.AnnoInfo,s.seriesByKey=new Map,s.seriesHash="?",s.newTracesBarCount=0,s.newTracesMapFirstNumber=0,s.yAxisTitleForScatterMapBox="",s.dataWarnings=[],s.doResize=r.default.debounce(function(){var e=window.getComputedStyle(s.graphDiv).display;if(e&&"none"!==e){var t=s.graphDiv.getBoundingClientRect();s.layout.width=t.width,s.layout.height=s.height,f.redraw(s.graphDiv),s.debug&&console.log("redraw with layout:",s.layout)}else console.warn("resize a plot that is not drawn yet")},50),s.deepCopyWithTemplates=function(e){if(r.default.isArray(e))return e.map(function(e){return s.deepCopyWithTemplates(e)});if(r.default.isString(e))return s.templateSrv.replace(e,s.panel.scopedVars);if(r.default.isObject(e)){var t={};return r.default.forEach(e,function(e,n){t[n]=s.deepCopyWithTemplates(e)}),t}return e},s._hadAnno=!1,s.initialized=!1,r.default.defaultsDeep(s.panel,s.defaultPanelConfigs),s.cfg=s.panel.pconfig,s.traces=[],s.events&&((0,l.loadPlotly)(s.cfg).then(function(e){f=e,s.debug&&console.log("Plotly",e),s.events.on("render",s.onRender.bind(s)),s.events.on("data-received",s.onDataReceived.bind(s)),s.events.on("data-error",s.onDataError.bind(s)),s.events.on("panel-size-changed",s.onResize.bind(s)),s.events.on("data-snapshot-load",s.onDataSnapshotLoad.bind(s)),s.events.on("refresh",s.onRefresh.bind(s)),s.refresh()}),s.events.on("init-edit-mode",s.onInitEditMode.bind(s)),s.events.on("panel-initialized",s.onPanelInitialized.bind(s))),s}return g(t,e),t.$inject=["$scope","$injector","$window","uiSegmentSrv","annotationsSrv"],t.prototype.getCssRule=function(e){for(var t=document.styleSheets,n=0;n<t.length;n+=1)for(var o=t[n].cssRules,r=0;r<o.length;r+=1){var i=o[r];if(i.selectorText===e)return i}return null},t.prototype.onResize=function(){this.debug&&console.log("onResize",this.graphDiv,this.layout,f,this.graphDiv&&this.layout&&f),this.graphDiv&&this.layout&&f&&this.doResize()},t.prototype.onDataError=function(e){this.series=[],this.annotations.clear(),this.render()},t.prototype.onRefresh=function(){this.otherPanelInFullscreenMode()||this.graphDiv&&this.initialized&&f&&f.redraw(this.graphDiv)},t.prototype.onInitEditMode=function(){var e=this;this.editor=new s.EditorHelper(this),this.addEditorTab("Display","public/plugins/natel-plotly-panel/partials/tab_display.html",2),this.addEditorTab("Queries","public/plugins/natel-plotly-panel/partials/tab_queries.html",3),this.onConfigChanged(),setTimeout(function(){e.debug&&console.log("RESIZE in editor"),e.onResize()},500)},t.prototype.processConfigMigration=function(){this.debug&&console.log("Migrating Plotly Configuration to version: "+t.configVersion);var e=this.panel.pconfig;if(delete e.layout.plot_bgcolor,delete e.layout.paper_bgcolor,delete e.layout.autosize,delete e.layout.height,delete e.layout.width,delete e.layout.margin,delete e.layout.scene,this.is3d()||delete e.layout.zaxis,e.settings.mode){var n=e.settings.mode,o={markers:0<=n.indexOf("markers"),lines:0<=n.indexOf("lines")};r.default.forEach(e.traces,function(e){e.show=o}),delete e.settings.mode}this.debug&&console.log("After Migration:",e),this.cfg=e,this.panel.version=t.configVersion},t.prototype.onPanelInitialized=function(){(!this.panel.version||t.configVersion>this.panel.version)&&this.processConfigMigration()},t.prototype.getProcessedLayout=function(){var e=this.deepCopyWithTemplates(this.cfg.layout);e.plot_bgcolor="transparent",e.paper_bgcolor=e.plot_bgcolor;var t=this.graphDiv.getBoundingClientRect();e.autosize=!1,e.height=this.height,e.width=t.width,e.xaxis||(e.xaxis={}),e.yaxis||(e.yaxis={}),this.cfg.fixScale&&("x"===this.cfg.fixScale?e.yaxis.scaleanchor="x":"y"===this.cfg.fixScale?e.xaxis.scaleanchor="y":"z"===this.cfg.fixScale&&(e.xaxis.scaleanchor="z",e.yaxis.scaleanchor="z"));var n=this.getCssRule("div.flot-text");if(n){var o=n.style.color;e.font||(e.font={}),e.font.color=o,o=i.default.color.parse(o).scale("a",.22).toString(),e.xaxis.gridcolor=o,e.yaxis.gridcolor=o}"scattermapbox"===this.cfg.settings.type&&(e.mapbox={domain:{x:[0,1],y:[0,.8]},center:{lon:-122.4,lat:37.75},style:"open-street-map",zoom:11},e.xaxis.domain=[0,1],e.yaxis.domain=[.85,1],e.yaxis.title=this.yAxisTitleForScatterMapBox),e.margin={l:e.yaxis.title?50:35,r:5,t:0,b:e.xaxis.title?65:30,pad:2},delete e.scene,delete e.zaxis,delete e.xaxis.range,delete e.yaxis.range;var r=this.graphDiv.layout;return r&&r.mapbox&&r.mapbox.center&&(e.mapbox.center=r.mapbox.center,e.mapbox.zoom=r.mapbox.zoom),r&&r.xaxis&&r.yaxis&&r.xaxis.range&&r.yaxis.range&&(e.xaxis.range=r.xaxis.range,e.yaxis.range=r.yaxis.range),r&&(e.dragmode=r.dragmode),e},t.prototype.drawPlot=function(){var e={showLink:!1,displaylogo:!1,displayModeBar:this.cfg.settings.displayModeBar,modeBarButtonsToRemove:["sendDataToCloud","lasso2d"]};this.layout=this.getProcessedLayout(),this.debug&&console.log("draw plot with","data",this.newTraces,"layout",this.layout,"options",e),f.react(this.graphDiv,this.newTraces,this.layout,e)},t.prototype.onPointsSelected=function(e){var t=this;e&&e.points&&e.points.length&&"bar"==e.points[0].data.type&&(this.pointsSelected={},e.points.forEach(function(e){return t.pointsSelected[e.x]=!0}),this.displayQueries())},t.prototype.onRender=function(){var e=this;if(!this.otherPanelInFullscreenMode()&&this.graphDiv&&f)if(this.initialized)this.initialized?f.redraw(this.graphDiv):this.debug&&console.log("Not initialized yet!");else{var t=function(t){e.newTraces.forEach(function(e){e.visible="legendonly"}),e.newTraces[t].visible="true",e.newTraces[t+e.newTracesMapFirstNumber].visible="true",e.yAxisTitleForScatterMapBox=e.cfg.queriesDescription[t].yaxistext,e.drawPlot()};"scattermapbox"===this.cfg.settings.type?t(0):this.drawPlot(),this.graphDiv.on("plotly_click",function(t){void 0!==t&&void 0!==t.points&&(e.onPointsSelected(t),e.debug&&console.log("on click",t))}),this.graphDiv.on("plotly_selected",function(t){void 0!==t&&void 0!==t.points&&(e.onPointsSelected(t),e.debug&&console.log("on select",t))}),"scattermapbox"===this.cfg.settings.type&&this.graphDiv.on("plotly_legendclick",function(n){var o=n.curveNumber;return o<e.newTracesBarCount?(t(o),!1):!(o<e.newTracesMapFirstNumber)}),this.graphDiv.on("plotly_legenddoubleclick",function(){return!1}),this.initialized=!0}},t.prototype.onDataSnapshotLoad=function(e){this.onDataReceived(e)},t.prototype.displayQueries=function(){var e=this,t=[],n=[],o=[],r=this.pointsSelected,i=function(e){return!r||e in r};this.dataList.forEach(function(n,r){var a;e.cfg.queriesDescription.forEach(function(e){Number(e.queryNumber)==r&&(a=e)});var s=a.queryTitle,l=a.columnNames,u=e.cfg.settings.type;if(n)if(a)if("scatter"===u||"bar"===u){var p=c.dataTransformator.toTraces(n,l),h=p.sortedSeries,d=p.allColumnNames;e.cfg.dataColumnNames.all=d,h.forEach(function(n){var o=n.x.map(String),r=n.y;t.push({x:o,y:r,type:e.cfg.settings.type,mode:e.cfg.settings.mode,fill:e.cfg.settings.fill,name:n.name})})}else if("scattermapbox"===u&&l.latColumn&&l.lonColumn&&l.dataColumn){var f=c.dataTransformator.toLatLonTraces(n,l,i),g=f.mapTrace,y=f.barTrace;d=f.allColumnNames,e.cfg.dataColumnNames.all=d,y.marker.color=a.color,y.name=s,t.push(y),g.marker.color=a.color,g.name=s,o.push(g)}else e.dataWarnings.push("UNEXPECTED GRAPH TYPE: "+u,"or lack of data configuration");else e.dataWarnings.push("no data description, can't display");else e.dataWarnings.push("no data, nothing to display")}),"scattermapbox"===this.cfg.settings.type&&(n.push({x:[0],y:[0],type:"scatter",name:"        ",visible:"legendonly",mode:"none"}),n.push({x:[0],y:[0],type:"scatter",name:"        ",visible:"legendonly",mode:"none"})),this.newTraces=[];var a=[];if(this.graphDiv.data)this.graphDiv.data.forEach(function(e){return a.push(e.visible)});else{var s=t.length+n.length+o.length;(a=new Array(s)).fill("true")}var l=0;t.forEach(function(t){t.visible=a[l],l++,e.newTraces.push(t)}),this.newTracesBarCount=l,n.forEach(function(t){t.visible=a[l],l++,e.newTraces.push(t)}),this.newTracesMapFirstNumber=l,o.forEach(function(t){t.visible=a[l],l++,e.newTraces.push(t)}),this.drawPlot()},t.prototype.onDataReceived=function(e){var t=this;if(this.dataWarnings=[],!e||e.length<1)this.debug&&console.log("data is empty:",e);else if(!this.cfg.queriesDescription||this.cfg.queriesDescription.length<1)this.debug&&console.log("queries discriptions are missing");else{this.debug&&(console.log("data received: dataList",e),console.log("data received: queriesDescription",this.cfg.queriesDescription)),this.dataList=e,this.displayQueries();var n=[],o="/";if(e&&0<e.length){var i=e.length===this.panel.targets.length;e.forEach(function(e,o){var s="";if(i&&((s=r.default.get(t.panel,"targets["+o+"].refId"))||(s=String.fromCharCode("A".charCodeAt(0)+o))),e.columns){for(var l=0;l<e.columns.length;l++)n.push(new a.SeriesWrapperTable(s,e,l));n.push(new a.SeriesWrapperTableRow(s,e))}else e.target?(n.push(new a.SeriesWrapperSeries(s,e,"value")),n.push(new a.SeriesWrapperSeries(s,e,"time")),n.push(new a.SeriesWrapperSeries(s,e,"index"))):console.error("Unsupported Series response",o,e)})}this.seriesByKey.clear(),n.forEach(function(e){e.getAllKeys().forEach(function(n){t.seriesByKey.set(n,e),o+="$"+n})}),this.series=n;var l=this.seriesHash!==o;l&&this.editor&&(s.EditorHelper.updateMappings(this),this.editor.selectTrace(this.editor.traceIndex),this.editor.onConfigChanged()),!l&&this.initialized||(this.onConfigChanged(),this.seriesHash=o);var u=Promise.resolve();!this.cfg.showAnnotations||this.is3d()?(this.annotations.clear(),this.layout&&(this.layout.shapes&&this.onConfigChanged(),this.layout.shapes=[])):u=this.annotationsSrv.getAnnotations({dashboard:this.dashboard,panel:this.panel,range:this.range}).then(function(e){var n=t.annotations.update(e);t.layout&&(n!==t._hadAnno&&t.onConfigChanged(),t.layout.shapes=t.annotations.shapes),t._hadAnno=n}),u.then(function(){t.render()})}},t.prototype.onConfigChanged=function(){var e=this;f&&(0,l.loadIfNecessary)(this.cfg).then(function(t){t&&(f&&f.purge(e.graphDiv),f=t),e.initialized&&e.graphDiv&&(e.cfg.showAnnotations||e.annotations.clear(),e.drawPlot()),e.render()})},t.prototype.is3d=function(){return"scatter3d"===this.cfg.settings.type},t.prototype.link=function(e,t,n,o){var r=this;this.graphDiv=t.find(".plotly-spot")[0],this.initialized=!1,t.on("mousemove",function(e){return r.mouse=e})},t.templateUrl="partials/module.html",t.configVersion=1,t}(o.MetricsPanelCtrl);t.PlotlyPanelCtrl=y,t.PanelCtrl=y},function(e,n){e.exports=t},function(e,t){e.exports=n},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),t.SeriesWrapperTable=t.SeriesWrapperTableRow=t.SeriesWrapperSeries=t.SeriesWrapper=void 0;var o,r,i="function"==typeof Symbol&&"symbol"==typeof Symbol.iterator?function(e){return typeof e}:function(e){return e&&"function"==typeof Symbol&&e.constructor===Symbol&&e!==Symbol.prototype?"symbol":typeof e},a=(o=n(0))&&o.__esModule?o:{default:o},s=(r=function(e,t){return(r=Object.setPrototypeOf||{__proto__:[]}instanceof Array&&function(e,t){e.__proto__=t}||function(e,t){for(var n in t)t.hasOwnProperty(n)&&(e[n]=t[n])})(e,t)},function(e,t){function n(){this.constructor=e}r(e,t),e.prototype=null===t?Object.create(t):(n.prototype=t.prototype,new n)}),l=function(){function e(e){this.refId=e}return e.$inject=["refId"],e.prototype.setFirst=function(e){this.first=e,a.default.isNumber(e)?this.type="number":a.default.isString(e)?this.type="string":(void 0===e?"undefined":i(e))===i(!0)&&(this.type="boolean")},e.prototype.getKey=function(){return this.name},e.prototype.getAllKeys=function(){return[this.getKey()]},e}(),u=function(e){function t(t,n,o){var r=e.call(this,t)||this;return r.series=n,r.value=o,r.count=n.datapoints.length,r.name=n.target,"index"===o?(r.first=0,r.type="number",r.name+="@index"):"value"===o?a.default.forEach(n.datapoints,function(e){return null===e[0]||(r.setFirst(e[0]),!1)}):"time"===o&&(r.type="epoch",r.first=n.datapoints[0][1],r.name+="@time"),r}return s(t,e),t.$inject=["refId","series","val"],t.prototype.toArray=function(){if("index"===this.value){for(var e=new Array(this.count),t=0;t<this.count;t++)e[t]=t;return e}var n="time"===this.value?1:0;return a.default.map(this.series.datapoints,function(e){return e[n]})},t.prototype.getAllKeys=function(){if(this.refId){var e=[this.name,this.refId+"@"+this.value,this.refId+"/"+this.name];return"A"===this.refId&&e.push("@"+this.value),e}return[this.name]},t}(t.SeriesWrapper=l);t.SeriesWrapperSeries=u;var c=function(e){function t(t,n){var o=e.call(this,t)||this;return o.table=n,o.name=t+"@row",o}return s(t,e),t.$inject=["refId","table"],t.prototype.toArray=function(){for(var e=this.table.rows.length,t=new Array(e),n=0;n<e;n++)t[n]=n;return t},t}(l);t.SeriesWrapperTableRow=c;var p=function(e){function t(t,n,o){var r=e.call(this,t)||this;r.table=n,r.index=o,r.count=n.rows.length;var i=n.columns[o];if(!i)throw new Error("Unkonwn Column: "+o);if(r.name=i.text,"time"===i.type)r.type="epoch",r.first=n.rows[0][o];else for(var a=0;a<r.count;a++){var s=n.rows[a][o];if(null!==s)return r.setFirst(s),r}return r}return s(t,e),t.$inject=["refId","table","index"],t.prototype.toArray=function(){var e=this;return a.default.map(this.table.rows,function(t){return t[e.index]})},t.prototype.getAllKeys=function(){return this.refId?[this.getKey(),this.refId+"/"+this.name,this.refId+"["+this.index+"]"]:[this.getKey()]},t}(l);t.SeriesWrapperTable=p},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),t.EditorHelper=void 0;var o,r=(o=n(0))&&o.__esModule?o:{default:o},i=n(1),a="-- remove --",s=function(){function e(t){this.ctrl=t,this.axis=new Array,this.debug=!1,this.traceIndex=0,this.queryIndex=0,this.mapping={},e.updateMappings(t),this.selectTrace(0),this.selectQuery(0)}return e.$inject=["ctrl"],e.updateMappings=function(e){if(null==e.series||e.series.length<1)return!1;var t={first:e.series[0].getKey(),time:e.series[1].getKey()},n=!1;return e.cfg.traces.forEach(function(o){r.default.defaults(o,i.defaultValues.defaultTrace);var a=o.mapping;a.color||(a.color=t.first,n=!0),a.x||(a.x=t.time,n=!0),a.y||(a.y=t.first,n=!0),e.is3d()&&!a.z&&(a.z=t.first,n=!0)}),n},e.prototype.onConfigChanged=function(){this.onUpdateAxis();for(var e=0;e<this.axis.length;e++)"between"===this.axis[e].layout.rangemode?r.default.isArray(this.axis[e].layout.range)||(this.axis[e].layout.range=[0,null]):delete this.axis[e].layout.range;this.ctrl.onConfigChanged()},e.prototype.onUpdateAxis=function(){if(this.trace.mapping){var e=this.ctrl.cfg.layout;e.xaxis||(e.xaxis={}),e.yaxis||(e.yaxis={}),this.axis=[],this.axis.push({label:"X Axis",layout:e.xaxis,property:"x",segment:this.mapping.x}),this.axis.push({label:"Y Axis",layout:e.yaxis,property:"y",segment:this.mapping.y}),this.ctrl.is3d()&&(e.zaxis||(e.zaxis={}),this.axis.push({label:"Z Axis",layout:e.zaxis,property:"z",segment:this.mapping.z}))}else console.error("Missing mappings for trace",this.trace)},e.prototype.selectQuery=function(e){this.queries=this.ctrl.cfg.queriesDescription,(!this.queries||this.queries.length<1)&&(this.queries=this.ctrl.cfg.queriesDescription=[r.default.cloneDeep(i.defaultValues.defaultQueryDescription)]),e>=this.queries.length&&(e=this.queries.length-1),this.query=this.queries[e],this.queryIndex=e,this.debug&&console.log("query select","editor:",this)},e.prototype.createQuery=function(){var e=r.default.cloneDeep(i.defaultValues.defaultQueryDescription),t=0;this.queries.forEach(function(e){var n=Number(e.queryNumber);t<n&&(t=n)}),0<this.queries.length&&(t+=1),e.queryNumber=t,this.ctrl.cfg.queriesDescription.push(e),this.selectQuery(t),this.ctrl.refresh(),this.debug&&console.log("query create","new",e,"editor:",this)},e.prototype.removeCurrentQuery=function(){if(this.query.queryNumber&&!(this.queries.length<1)){for(var e=0;e<this.ctrl.cfg.queriesDescription.length;e++)if(this.query.queryNumber===this.ctrl.cfg.queriesDescription[e].queryNumber)return this.ctrl.cfg.queriesDescription.splice(e,1),e>=this.ctrl.cfg.queriesDescription.length&&(e=this.ctrl.cfg.queriesDescription.length-1),this.selectQuery(e),this.ctrl.refresh(),void(this.debug&&console.log("query remove","editor:",this));this.ctrl.dataWarnings.push("can't delete selected query.")}},e.prototype.selectTrace=function(t){var n=this;this.traces=this.ctrl.cfg.traces,(!this.traces||this.traces.length<1)&&(this.traces=this.ctrl.cfg.traces=[r.default.cloneDeep(i.defaultValues.defaultTrace)]),t>=this.ctrl.cfg.traces.length&&(t=this.ctrl.cfg.traces.length-1),this.trace=this.ctrl.cfg.traces[t],this.traceIndex=t,r.default.defaults(this.trace,i.defaultValues.defaultTrace),this.trace.name||(this.trace.name=e.createTraceName(t)),this.symbol=this.ctrl.uiSegmentSrv.newSegment({value:this.trace.settings.marker.symbol}),this.mapping={},r.default.forEach(this.trace.mapping,function(e,t){n.updateSegMapping(e,t)}),this.onConfigChanged(),this.ctrl.refresh()},e.prototype.updateSegMapping=function(e,t,n){if(void 0===n&&(n=!1),a===e)this.mapping[t]=this.ctrl.uiSegmentSrv.newSegment({value:"Select Metric",fake:!0}),e=null;else if(e){var o=this.ctrl.seriesByKey.get(e),r={value:e,series:o};o||(r.html=e+'  <i class="fa fa-exclamation-triangle"></i>'),this.mapping[t]=this.ctrl.uiSegmentSrv.newSegment(r)}else this.mapping[t]=this.ctrl.uiSegmentSrv.newSegment({value:"Select Metric",fake:!0});n&&(this.trace.mapping[t]=e)},e.prototype.createTrace=function(){var t={};(t=0<this.ctrl.cfg.traces.length?r.default.cloneDeep(this.ctrl.cfg.traces[this.ctrl.cfg.traces.length-1]):r.default.cloneDeep(i.defaultValues.defaultTrace)).name=e.createTraceName(this.ctrl.traces.length),this.ctrl.cfg.traces.push(t),this.selectTrace(this.ctrl.cfg.traces.length-1)},e.prototype.removeCurrentTrace=function(){if(this.traces.length<=1)console.error("Wont remove a single trace",this);else{for(var e=0;e<this.traces.length;e++)if(this.trace===this.traces[e])return this.traces.splice(e,1),e>=this.traces.length&&(e=this.traces.length-1),this.ctrl.onConfigChanged(),this.selectTrace(e),void this.ctrl.refresh();console.error("Could not find",this)}},e.createTraceName=function(e){return"Trace "+(e+1)},e.prototype.getSeriesSegs=function(e){var t=this;return void 0===e&&(e=!1),new Promise(function(n,o){var r=[];e&&r.push(t.ctrl.uiSegmentSrv.newSegment({fake:!0,value:a,series:null})),t.ctrl.series.forEach(function(e){r.push(t.ctrl.uiSegmentSrv.newSegment({value:e.name,series:e}))}),n(r)})},e.prototype.onAxisSeriesChanged=function(e){this.updateSegMapping(e.segment.value,e.property,!0),this.onConfigChanged()},e.prototype.getTextSegments=function(){return[this.mapping.text]},e.prototype.onTextMetricChanged=function(e){var t=this.mapping.text;this.updateSegMapping(t.value,"text",!0),this.onConfigChanged()},e.prototype.getColorSegments=function(){return"ramp"===this.trace.settings.color_option?[this.mapping.color]:[]},e.prototype.onColorChanged=function(){var e=this.mapping.color;this.updateSegMapping(e.value,"color",!0),this.onConfigChanged()},e.prototype.onSymbolChanged=function(){this.trace.settings.marker.symbol=this.symbol.value,this.onConfigChanged()},e.prototype.getSymbolSegs=function(){var e=this;return new Promise(function(t,n){var o=[];r.default.forEach(["circle","circle-open","circle-dot","circle-open-dot","square","square-open","square-dot","square-open-dot","diamond","diamond-open","diamond-dot","diamond-open-dot","cross","cross-open","cross-dot","cross-open-dot","x","x-open","x-dot","x-open-dot","triangle-up","triangle-up-open","triangle-up-dot","triangle-up-open-dot","triangle-down","triangle-down-open","triangle-down-dot","triangle-down-open-dot","triangle-left","triangle-left-open","triangle-left-dot","triangle-left-open-dot","triangle-right","triangle-right-open","triangle-right-dot","triangle-right-open-dot","triangle-ne","triangle-ne-open","triangle-ne-dot","triangle-ne-open-dot","triangle-se","triangle-se-open","triangle-se-dot","triangle-se-open-dot","triangle-sw","triangle-sw-open","triangle-sw-dot","triangle-sw-open-dot","triangle-nw","triangle-nw-open","triangle-nw-dot","triangle-nw-open-dot","pentagon","pentagon-open","pentagon-dot","pentagon-open-dot","hexagon","hexagon-open","hexagon-dot","hexagon-open-dot","hexagon2","hexagon2-open","hexagon2-dot","hexagon2-open-dot","octagon","octagon-open","octagon-dot","octagon-open-dot","star","star-open","star-dot","star-open-dot","hexagram","hexagram-open","hexagram-dot","hexagram-open-dot","star-triangle-up","star-triangle-up-open","star-triangle-up-dot","star-triangle-up-open-dot","star-triangle-down","star-triangle-down-open","star-triangle-down-dot","star-triangle-down-open-dot","star-square","star-square-open","star-square-dot","star-square-open-dot","star-diamond","star-diamond-open","star-diamond-dot","star-diamond-open-dot","diamond-tall","diamond-tall-open","diamond-tall-dot","diamond-tall-open-dot","diamond-wide","diamond-wide-open","diamond-wide-dot","diamond-wide-open-dot","hourglass","hourglass-open","bowtie","bowtie-open","circle-cross","circle-cross-open","circle-x","circle-x-open","square-cross","square-cross-open","square-x","square-x-open","diamond-cross","diamond-cross-open","diamond-x","diamond-x-open","cross-thin","cross-thin-open","x-thin","x-thin-open","asterisk","asterisk-open","hash","hash-open","hash-dot","hash-open-dot","y-up","y-up-open","y-down","y-down-open","y-left","y-left-open","y-right","y-right-open","line-ew","line-ew-open","line-ns","line-ns-open","line-ne","line-ne-open","line-nw","line-nw-open"],function(t){o.push(e.ctrl.uiSegmentSrv.newSegment(t))}),t(o)})},e}();t.EditorHelper=s},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),t.loadPlotly=l,t.loadIfNecessary=function(e){return r?s===e.loadFromCDN?"scatter"===e.settings.type||a?Promise.resolve(null):(console.log("Switching to the full plotly library"),r=null,l(e)):(console.log("Use CDN",e.loadFromCDN),r=null,l(e)):l(e)};var o,r,i=(o=n(8))&&o.__esModule?o:{default:o},a=!1,s=!1;function l(e){if(r)return Promise.resolve(r);var t="scatter"!==e.settings.type,n="public/plugins/natel-plotly-panel/lib/plotly-cartesian.min.js";return e.loadFromCDN?n=t?"https://cdn.plot.ly/plotly-latest.min.js":"https://cdn.plot.ly/plotly-cartesian-latest.min.js":t&&(n="public/plugins/natel-plotly-panel/lib/plotly.min.js"),new Promise(function(e,t){(0,i.default)(n,e)}).then(function(n){return a=t,s=e.loadFromCDN,r=window.Plotly})}},function(e,t,n){var o,r,i;
/*!
  * $script.js JS loader & dependency manager
  * https://github.com/ded/script.js
  * (c) Dustin Diaz 2014 | License MIT
  */
/*!
  * $script.js JS loader & dependency manager
  * https://github.com/ded/script.js
  * (c) Dustin Diaz 2014 | License MIT
  */i=function(){var e,t,n=document,o=n.getElementsByTagName("head")[0],r=!1,i="push",a="readyState",s="onreadystatechange",l={},u={},c={},p={};function h(e,t){for(var n=0,o=e.length;n<o;++n)if(!t(e[n]))return r;return 1}function d(e,t){h(e,function(e){return t(e),1})}function f(t,n,o){t=t[i]?t:[t];var r=n&&n.call,a=r?n:o,s=r?t.join(""):n,y=t.length;function m(e){return e.call?e():l[e]}function v(){if(!--y)for(var e in l[s]=1,a&&a(),c)h(e.split("|"),m)&&!d(c[e],m)&&(c[e]=[])}return setTimeout(function(){d(t,function t(n,o){return null===n?v():(o||/^https?:\/\//.test(n)||!e||(n=-1===n.indexOf(".js")?e+n+".js":e+n),p[n]?(s&&(u[s]=1),2==p[n]?v():setTimeout(function(){t(n,!0)},0)):(p[n]=1,s&&(u[s]=1),void g(n,v)))})},0),f}function g(e,r){var i,l=n.createElement("script");l.onload=l.onerror=l[s]=function(){l[a]&&!/^c|loade/.test(l[a])||i||(l.onload=l[s]=null,i=1,p[e]=2,r())},l.async=1,l.src=t?e+(-1===e.indexOf("?")?"?":"&")+t:e,o.insertBefore(l,o.lastChild)}return f.get=g,f.order=function(e,t,n){!function o(r){r=e.shift(),e.length?f(r,o):f(r,t,n)}()},f.path=function(t){e=t},f.urlArgs=function(e){t=e},f.ready=function(e,t,n){e=e[i]?e:[e];var o,r=[];return!d(e,function(e){l[e]||r[i](e)})&&h(e,function(e){return l[e]})?t():(o=e.join("|"),c[o]=c[o]||[],c[o][i](t),n&&n(r)),f},f.done=function(e){f([null],e)},f},e.exports?e.exports=i():void 0===(r="function"==typeof(o=i)?o.call(t,n,t,e):o)||(e.exports=r)},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var o=function(){return(o=Object.assign||function(e){for(var t,n=1,o=arguments.length;n<o;n++)for(var r in t=arguments[n])Object.prototype.hasOwnProperty.call(t,r)&&(e[r]=t[r]);return e}).apply(this,arguments)},r=function(){function e(){this.clear()}return e.prototype.clear=function(){this.shapes=[],this.trace={mode:"markers",type:"scatter",hoverinfo:"x+text",x:[],y:[],text:[],yaxis:"y2",marker:{size:15,symbol:"triangle-up",color:[]}}},e.prototype.update=function(e){if(!e||!e.annotations)return this.clear(),!1;var t=[],n=[],r=[],i=[];return this.shapes=e.annotations.map(function(e){return t.push(e.time),n.push(0),r.push(e.text),i.push(e.annotation.iconColor),{type:"line",xref:"x",yref:"paper",x0:e.time,y0:0,x1:e.time,y1:1,visible:!0,layer:"above",fillcolor:e.annotation.iconColor,opacity:.8,line:{color:e.annotation.iconColor,width:1,dash:"dash"}}}),this.trace=o({},this.trace,{x:t,y:n,text:r}),this.trace.marker.color=i,0<t.length},e}();t.AnnoInfo=r},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),t.dataTransformator=void 0;var o=n(11),r=function(){function e(){}return e.normalize=function(e,t,n){if(!e||e.length<1||n<t)return e;var o=e[0],r=e[0];e.forEach(function(e){r<e&&(r=e),e<o&&(o=e)});var i=r-o,a=n-t;return 0!=i?e.map(function(e){return 0==e?0:(e-o)/i*a+t}):e.map(function(e){return 0==e?0:t})},e.toLatLonTraces=function(t,n,o){this.debug&&(console.log(this.ident,"whole dataSet",t),console.log(this.ident,"data columns",n));var r=function(){function e(e,t,n){this.lat=0,this.lon=0,this.data=0,this.key="",this.lat=e,this.lon=t,this.data=n,this.key=this.MapKey(e,t)}return e.prototype.MapKey=function(e,t){return e.toString()+":"+t.toString()},e}(),i=new Map,a=new Map,s="";if(t.rows&&0<t.rows.length){s=t.columns.map(function(e){return e.text}).join(" ");var l=0,u=0,c=0,p=0;t.columns.forEach(function(e,t){e.text==n.xColumn&&(u=t),e.text==n.latColumn&&(c=t),e.text==n.lonColumn&&(p=t),e.text==n.dataColumn&&(l=t)}),this.debug&&(console.log(this.ident,"column names",n),console.log(this.ident,"columns: data",l,"x",u,"lat",c,"lon",p)),t.rows.forEach(function(e){var t=e[l],n=Number(e[l]),s=e[u],h=Number(e[u]),d=e[p],f=e[c];if(s&&t&&((m=i.get(h))?i.set(h,n+m):i.set(h,n)),d&&f&&t&&s&&o(s)){var g=Number(d),y=Number(f),m=new r(y,g,n),v=a.get(m.key);v?v.data=v.data+m.data:v=m,a.set(v.key,v)}})}var h=a.size,d=new Array(h),f=new Array(h),g=new Array(h),y=0;a.forEach(function(e){d[y]=e.lat,f[y]=e.lon,g[y]=e.data,y++}),this.debug&&(console.log(this.ident,"map points",a,"data",g),console.log(this.ident,"graph points",i));var m=24;i.forEach(function(e,t){m<t&&(m=t)}),h=m+1;for(var v=new Array(h).map(function(){return 0}),x=new Array(h).map(function(){return 0}),b=new Array(h).map(function(){return 0}),w=0;w<h;w++)v[w]=w,x[w]=0,b[w]=1;i.forEach(function(e,t){o(t)||(b[t]=.4),x[t]=e});var S={x:v,y:x,type:"bar",marker:{opacity:b},text:x},C=e.normalize(g,20,50);return Object({mapTrace:{type:"scattermapbox",lon:f,lat:d,opacity:.6,marker:{size:C},text:g},barTrace:S,allColumnNames:s})},e.toTraces=function(e,t){this.debug&&(console.log(this.ident,"whole dataSet",e),console.log(this.ident,"data columns",t));var n=new Map,r=[],i="";if(e.rows&&0<e.rows.length){i=e.columns.map(function(e){return e.text}).join(" ");var a=2,s=1,l=3;e.columns.forEach(function(e,n){e.text==t.xColumn&&(s=n),e.text==t.yColumn&&(l=n),e.text==t.dataColumn&&(a=n)}),e.rows.sort(function(e,t){var n=Number(e[s]),o=Number(t[s]);return o<n?1:n<o?-1:0}).forEach(function(e){var r=e[a],i=Number(e[s]),u=Number(e[l]);"Time"==t.xColumn&&(i=new Date(e[s]).getHours());var c=n.get(r);c||((c=new o.Trace).name=r,n.set(r,c));var p=c.x[c.x.length-1];i<=p&&(i=p+1),c.x.push(i),c.y.push(u)}),r=Array.from(n.values()).sort(function(e,t){return e.name.localeCompare(t.name,void 0,{numeric:!0})})}return this.debug&&console.log(this.ident,"sorted series",r),Object({sortedSeries:r,allColumnNames:i})},e.ident="dataTransformator",e.debug=!1,e}();t.dataTransformator=r},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});t.Trace=function(){this.name="",this.x=[],this.y=[]}}])});
//# sourceMappingURL=module.js.map