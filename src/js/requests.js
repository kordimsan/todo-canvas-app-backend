function get_request_uvhd(url, body, stend) {
    $http.config({});
    var auth = "";
    var base_url = "";
    if (stend == "MT") {
        auth = "Basic TkxQX0lOVEVHUkFUOlF3ZXJ0eTEyMw==";
        base_url = "http://tpldd-urx01.delta.sbrf.ru:8070";
    }
    else if (stend == "DEV") {
        auth = "Basic TkxQX0lOVEVHUkFUOnVybWVkYzM0NQ==";
        base_url = "http://tpldd-urm01.delta.sbrf.ru:8074";
    }
    else if (stend == "IFT") {
        auth = "Basic TkxQX0lOVEVHUkFUOjEyMzQ1Njc=";
        base_url = "http://sbt-oasap-576.delta.sbrf.ru:8041";
    }
    var response = $http.get(base_url + url, {
        "headers": {
            "content-type": "application/json",
            "authorization": auth
        },
        "body": body
    });
    $jsapi.log("Profile response: " + JSON.stringify(response));
    return response;
}

function post_request_mws(url, body) {
    $http.config({
        authService: {
            service: 'tls',
            cert: '$mws_cert',
            key: '$mws_key'
        }
    });
    var response = $http.post(url, {
        "headers": {
            "content-type": "application/json"
        },
        "body": body
    });
    $jsapi.log("Mws response: " + JSON.stringify(response));
    return response;
}

function post_request_mws_dev(url, body, token) {
    return $http.post(url, {
        "headers": {
            "Model-Token": token
        },
        "body": body
    });
}

function get_token_mws(url) {
    return $http.get(url);
}