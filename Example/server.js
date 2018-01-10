var express = require('express')
var bodyParser = require('body-parser');

var updateGraphs = {};
var components = {};

var app = express();
app.use(bodyParser.json({limit: '50mb'}));
app.get('/updateGraph', function (req, res) {
    console.log(updateGraphs);
    var appVersion = req.query["appVersion"];
    if (updateGraphs[appVersion]) {
        var updateGraph = updateGraphs[appVersion];
        var responseBody = {
            updateGraph: updateGraph
        };
        res.send(JSON.stringify(responseBody));
    } else {
        res.sendStatus(204);
    }
});

app.post('/getComponents', function (req, res) {
    var body = req.body;
    var componentsRequested = body.components;
    if (!componentsRequested) {
        res.sendStatus(400);
    } else {
        var componentsFetched = {};
        componentsRequested.forEach(function (componentKey) {
            if (components[componentKey]) {
                componentsFetched[componentKey]=components[componentKey];
            }
        });
        var responseBody = {
            components: componentsFetched
        };
        res.send(JSON.stringify(responseBody));
    }
});

app.post('/addComponents', function (req, res) {
    var body = req.body;
    var componentsAdded = body.components;
    components = Object.assign({}, components, componentsAdded);
    res.sendStatus(200);
});

app.post('/updateGraph', function (req, res) {
    var appVersion = req.query["appVersion"];
    var body = req.body;
    console.log(req.body);
    updateGraphs[appVersion] = body.updateGraph;
    res.sendStatus(200);
});

var server = app.listen(process.env.PORT || 8080, function () {
    // var host = server.address().address;
    var port = server.address().port;

    console.log('Server listening on port %s', port);
});