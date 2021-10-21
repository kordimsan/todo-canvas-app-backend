theme: /
    state: getCir
        state: 0
            script:
                var params = {
                    "id": 2,
                    "tb": $session.tb,
                    "year": parseInt($session.year),
                    "month": parseInt($session.month)
                };
                $session.response = getData(params);
                $session.n = 0
                $session.title = "Влияние блоков на CIR"
            if: $session.response.status == 200
                go!: /showCir
            else:
                go!: /errorNode
        state: 1
            script:
                var params = {
                    "id": 2,
                    "tb": $session.tb,
                    "year": parseInt($session.year),
                    "month": parseInt($session.month)
                };
                $session.response = getData(params);
                $session.n = 1
                $session.title = "Влияние блоков на CIR"
            if: $session.response.status == 200
                go!: /showCir
            else:
                go!: /errorNode
        state: 2
            script:
                var params = {
                    "id": 2,
                    "tb": $session.tb,
                    "year": parseInt($session.year),
                    "month": parseInt($session.month)
                };
                $session.response = getData(params);
                $session.n = 2
                $session.title = "Влияние ГОCБ на CIR"
            if: $session.response.status == 200
                go!: /showCir
            else:
                go!: /errorNode

    state: showCir
        script:
            var $session = $jsapi.context().session;
            var texts = $session.response.data.result.texts
            var reply = {"type":"text", "text":texts[$session.n]};
            $response.replies = $response.replies || [];
            $response.replies.push(reply);
            
            var tables = $session.response.data.result.tables
            
            var cells = [];
            //for (var t in tables) {
            var table = tables[$session.n]
            for (var cell in table) {
                for (var row in table[cell]) {
                    cells.push({
                        "title":table[cell][row][0],
                        "value": "Факт: " + table[cell][row][2] + "%, План: " + table[cell][row][3] + "%",
                        "subtitle": cell + ": " + table[cell][row][1] + " п.п."
                    });
                }
            }
            //}
            var reply = {
                "type": "cardList",
                "title": $session.title,
                "subtitle": "",
                "cells": cells,
                "auto_listening": false
            };
            $response.replies = $response.replies || [];
            $response.replies.push(reply);
        
        if: $session.n == 0
            buttons:
                "Подробнее по блокам" -> /getCir/1
        buttons:
            "Назад" -> /getPreview
        buttons:
            "Завершить диалог" -> /finish