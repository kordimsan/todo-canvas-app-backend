function post_request_mws_dev(url, body) {
    // $http.config({
    //     authService: {
    //         service: 'tls',
    //         cert: '$mws_cert',
    //         key: '$mws_key'
    //     }
    // });
    var response = $http.post(url, {
        "headers": {
            "content-type": "application/json"
        },
        "body": body
    });
    $jsapi.log("Mws response: " + JSON.stringify(response));
    return response;
}

function getToken() {
    var url = "https://devdocker4.vm.mos.cloud.sbrf.ru:8080/models/dfsa-autocomment/token";
    var response = $http.query(url, {method: "GET"});
    $jsapi.log("RESP INFO: " + JSON.stringify(response.data));
    return response.isOk ? response.data : false;
}

function getData(params) {
    // var url = "https://dfsa-autocomment-1-10000.ci00324559-dev-mws.apps.test-ose.ca.sbrf.ru/dfsa-autocomment/predict";
    var url = "https://boiling-oasis-65608.herokuapp.com/predict";
    
    var response = post_request_mws_dev(url, params);
    $jsapi.log("RESP INFO: " + JSON.stringify(response.data));
    return response //.isOk ? response.data : response.error;
}

function getTable(table) {
    var res = []
    for (var row in table) {
        if (Array.isArray(table[row])) {
            var val = table[row].join(', ')
        } else {
            var val = table[row]
        }
        res.push({
        "title":row,
        "value":val,
        "subtitle":"",
        "iconUrl":"","hash":"","action":{"name": row}});
    }
    return res
}

function getRole(roles) {
    var $session = $jsapi.context().session;
    var $phrase = $jsapi.context().request.query;
    
    $session.search_type = "Не определено";
    $session.role = "Не определено";
    $session.search_value = "Не определено"
    
    for (var i = 1, element; element = roles[String(i)]; i++) {
        // проверяем по коду транзакции
        if ($phrase.toLowerфCase().indexOf(element["value"]["transaction"].toLowerCase()) != -1) {
            $session.search_value = element["value"]["transaction"];
            $session.search_type = "Транзакция"
            break;
        }
        // проверяем по коду роли
        var code_regex = new RegExp(element["value"]["code_regex"].toLowerCase())
        
        if ($phrase.toLowerCase().search(code_regex) != -1) {
            $session.search_value = element["value"]["code"];
            $session.role = element["value"]["code"];
            $session.search_type = "Код роли"
            break;
        }
        // проверяем по названию роли
        var code_regex = new RegExp(element["value"]["name_regex"].toLowerCase())
        
        if ($phrase.toLowerCase().search(code_regex) != -1) {
            $session.search_value = element["value"]["name"];
            $session.role = element["value"]["code"];
            $session.search_type = "Название роли"
            break;
        }
    }
}