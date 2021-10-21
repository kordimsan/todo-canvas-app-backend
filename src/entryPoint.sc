require: js/mws_query.js
require: scenario/autocomment/cir.sc
require: scenario/autocomment/rank.sc
require: scenario/autocomment/expenses.sc
require: scenario/autocomment/preview.sc
require: scenario/autocomment/smotr-konkurs.sc

require: js/cards.js
require: js/requests.js
require: js/roles.js
require: scenario/find_roles/find_roles.sc

patterns:
    $expCategory = $regexp<.*>

theme: /
    state: start
        q!: * *start
        a: Привет, Мария!
        script:
            $session.token = getToken();
        
            
        go: /getPreview
    
    state: init_find_roles
        intent!: /find_roles
        go!: /get_role_list
    
    state: init_preview
        intent!: /cir
        script: $session.scenario = 1
        script:
            $session.token = getToken();
            $session.year = $jsapi.dateForZone("Europe/Moscow","yyyy");
            $session.month = $jsapi.dateForZone("Europe/Moscow","MM");
            $session.month = "07";
        script:
            $session.tb = "Байкальский банк";
        go!: /getPreview
    
    state: init_rank
        intent!: /rank
        script: $session.scenario = 6
        script:
            $session.token = getToken();
            $session.year = $jsapi.dateForZone("Europe/Moscow","yyyy");
            $session.month = $jsapi.dateForZone("Europe/Moscow","MM");
            $session.month = "07";
        script:
            $session.tb = "Байкальский банк";
        go!: /getRank

    state: init_expenses
        intent!: /expenses
        script: $session.scenario = 3
        script:
            $session.token = getToken();
            $session.year = $jsapi.dateForZone("Europe/Moscow","yyyy");
            $session.month = $jsapi.dateForZone("Europe/Moscow","MM");
            $session.month = "07";
        script:
            $session.tb = "Байкальский банк";
        go!: /getExpenses
    
    state: getPeriod
        script:
            var $session = $jsapi.context().session;
            $response.replies = $response.replies || [];
            var reply = {"type":"text", "text": "За какой период нужен анализ? Выберите или напишите ваш вариант"};
            $response.replies.push(reply);
        
        
        
        buttons:
            "Последний завершенный год" -> lastYear
        buttons:
            "Последний завершенный квартал" -> lastQuarter
        buttons:
            "Первое полугодие" -> firtsHalfYear
        
        state: 4
            q!: * @duckling.date *
            script:
                $session.year = $parseTree.value.year;
                $session.month = ("0" + $parseTree.value.month).slice(-2);
            go!: /getPreview
        
        state: lastYear
            script:
                $session.year = $jsapi.dateForZone("Europe/Moscow","yyyy")-1;
                $session.month = "12";
            go!: /getPreview
            
        state: lastQuarter
            script:
                $session.year = $jsapi.dateForZone("Europe/Moscow","yyyy");
                $session.month = "06";
            go!: /getPreview
            
        state: firtsHalfYear
            script:
                $session.year = $jsapi.dateForZone("Europe/Moscow","yyyy");
                $session.month = "06";
            go!: /getPreview
        
    state: showPeriod
        a: {{$session.year}} - {{$session.month}}
    
    state: errorNode
        script:
            $jsapi.log($session)
        a: Ошибка {{$session.response.status}}
            {{$session.response.error.error}}
    
    state: finish
        a: Спасибо за визит!
        EndSession: