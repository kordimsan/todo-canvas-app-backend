theme: /
    state: getExpenses
        script:
            var params = {
                "id": 3,
                "tb": $session.tb,
                "year": parseInt($session.year),
                "month": parseInt($session.month)
            };
            $session.response = getData(params);
            
        if: $session.response.status == 200
            go!: /showExpenses
        else:
            go!: /errorNode

    state: showExpenses
        script:
            var $session = $jsapi.context().session;
            var texts = $session.response.data.result.texts
            var reply = {"type":"text", "text":texts.join("\n")};
            $response.replies = $response.replies || [];
            $response.replies.push(reply);
            
            var tables = $session.response.data.result.tables
            
            var table = tables[0];
            
            var cells = [];
            for (var cell in table) {
                cells.push({
                    "title":cell,
                    "value":"Факт: " + table[cell]["Факт"].join(" "),
                    "subtitle":"План: " + table[cell]["План"].join(" ") + " Отклонение:" + table[cell]["Отклонение от плана"].join(" ") + "(" + table[cell]["Отклонение от плана, %"].join("") + ")",
                    "action": {
                        "text": "Подробнее о категории " + cell
                    },
                });
            }
            var reply = {
                "type": "cardList",
                "title": "Расходы по категориям",
                "subtitle": "",
                "cells": cells,
                "auto_listening": false
            };
            $response.replies = $response.replies || [];
            $response.replies.push(reply);
            
        buttons:
            "Назад" -> /getPreview
        buttons:
            "Завершить диалог" -> /finish
    
    state: detailsExpense
        q!: Подробнее о категории $expCategory
        script:
            var $session = $jsapi.context().session;
            var tables = $session.response.data.result.tables
            var table = tables[0];
            var row = table[$parseTree._expCategory]
            var cells = [];
            for (var cell in row) {
                cells.push({
                    "title":cell,
                    "subtitle":"",
                    "value":row[cell],
                });
            }
            var reply = {
                "type": "cardList",
                "title": $parseTree._expCategory,
                "subtitle": "",
                "cells": cells,
                "auto_listening": false
            };
            $response.replies = $response.replies || [];
            $response.replies.push(reply);
            
        buttons:
            "Назад" -> /showExpenses
        buttons:
            "Завершить диалог" -> /finish