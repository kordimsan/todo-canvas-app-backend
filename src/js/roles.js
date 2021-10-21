function createGraph(be) {
    var graph = {
        "be": be,
        "number": 0,
        "adjacentList": {}
    };
    return graph;
}

function graphAddAnalytic(graph, analytic) {
    graph["adjacentList"][analytic] = [];
    graph["number"]++;
}

function graphAddEdge(graph, analytic1, analytic2) {
    //graph["adjacentList"][analytic1].push(analytic2);
    graph["adjacentList"][analytic2].push(analytic1);
}

function graphHasAnalytic(graph, analytic) {
    return analytic in graph["adjacentList"];
}

function graphShowConnections(graph) {
    var connectionsTxt = "";
    var allAnalytics = Object.keys(graph["adjacentList"]);
    allAnalytics.forEach(function(analytic) {
        var analyticConnections = graph["adjacentList"][analytic];
        var connections = "";
        var vertex;
        analyticConnections.forEach(function(vertex) {
            connections += vertex + " ";
        });
        connectionsTxt = connectionsTxt + analytic + "-->" + connections + "<br>";
    });
    return connectionsTxt;
}

function dfsTxt(graph, start, visited, txt, level) {
    level++;
    var levelStr = "";
    for (var i = 0; i < level; i++) {
        levelStr += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
    }
    txt += levelStr + start + "<br>";
    visited.push(start);
    var childrenNodes = graph["adjacentList"][start];
    childrenNodes.forEach(function(child){
        if (!(child in visited)) {
            txt = dfsTxt(graph, child, visited, txt, level);
        }
    });
    return txt;
}

function dfsAnalytics(graph, start, visited, analytics) {
    analytics.push(start);
    visited.push(start);
    var childrenNodes = graph["adjacentList"][start];
    childrenNodes.forEach(function(child){
        if (!(child in visited)) {
            analytics = dfsAnalytics(graph, child, visited, analytics);
        }
    });
    return analytics;
}

function graphDfs(graph, start, return_text) {
    var visited = [];
    if (return_text == true) {
        var txt = "";
        var level = -1;
        return dfsTxt(graph, start, visited, txt, level);
    }
    else {
        var analytics = [];
        return dfsAnalytics(graph, start, visited, analytics);
    }
}

function createSingleGraphForRole(role) {
    var graph = {};
    var role_param_val = role.toParamVal.results;
    var role_analytics_mapping = {};
    // Создаем маппинг код аналитики->название аналитики
    role.toParamDef.results.forEach(function(analytic){
        role_analytics_mapping[analytic.AgrParam] = analytic.ParName;
    });
    graph = createGraph("BUKRS");
    graphAddAnalytic(graph, role_analytics_mapping["BUKRS"]);
    // проход для формирования вершин и ребер
    role_param_val.forEach(function(analytic){
        if (analytic.AgrParam != "BUKRS") {
            // нужная нам аналитика
            var current_analytic = role_analytics_mapping[analytic.AgrParam];
            // связанная аналитика
            var adjacent_analytic = role_analytics_mapping[analytic.ParentAgrParam];
            // если в графе еще нет таких вершин, добавляем их
            if (!graphHasAnalytic(graph, current_analytic)) {
                graphAddAnalytic(graph, current_analytic);
            }
            if (!graphHasAnalytic(graph, adjacent_analytic)) {
                graphAddAnalytic(graph, adjacent_analytic);
            }
            // добавляем ребро
            graphAddEdge(graph, current_analytic, adjacent_analytic);
        }
    });
    return graph;
}

function createGraphsForRole(role) {
    var be_graphs = {};
    var role_param_val = role.toParamVal.results;
    var role_analytics_mapping = {};
    // Создаем маппинг код аналитики->название аналитики
    role.toParamDef.results.forEach(function(analytic){
        role_analytics_mapping[analytic.AgrParam] = analytic.ParName;
    });
    // проход для создания графов по БЕ
    role_param_val.forEach(function(analytic){
        if (analytic.AgrParam == "BUKRS") {
            var graph = createGraph(analytic.ParValue);
            graphAddAnalytic(graph, role_analytics_mapping["BUKRS"] + " " + analytic.ParValue);
            be_graphs[graph["be"]] = graph;
        }
    });
    // проход для формирования вершин и ребер
    role_param_val.forEach(function(analytic){
        if (analytic.AgrParam != "BUKRS") {
            // получаем нужный граф
            var graph = be_graphs[analytic.Bukrs];
            // нужная нам аналитика
            var current_analytic = role_analytics_mapping[analytic.AgrParam] + " " + analytic.ParValue;
            // связанная аналитика
            var adjacent_analytic = role_analytics_mapping[analytic.ParentAgrParam] + " " + analytic.ParentParValue;
            // если в графе еще нет таких вершин, добавляем их
            if (!graphHasAnalytic(graph, current_analytic)) {
                graphAddAnalytic(graph, current_analytic);
            }
            if (!graphHasAnalytic(graph, adjacent_analytic)) {
                graphAddAnalytic(graph, adjacent_analytic);
            }
            // добавляем ребро
            graphAddEdge(graph, current_analytic, adjacent_analytic);
        }
    });
    
    return be_graphs;
}

function sortRoles(roles, roles_order) {
    for (var i = 1; i < roles.length; i++) {
        var current_rank = roles_order[parseInt(roles[i].AgrNum, 10)];
        var previous_rank = roles_order[parseInt(roles[i-1].AgrNum, 10)];
        if (current_rank > previous_rank) {
            var temp = roles[i-1];
            roles[i-1] = roles[i];
            roles[i] = temp;
        }
    }
}