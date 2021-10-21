theme: /
    state: getRank
        script:
            var params = {
                "id": 6,
                "tb": $session.tb,
                "year": parseInt($session.year),
                "month": parseInt($session.month)
            };
            $session.response = getData(params);
            
        if: $session.response.status == 200
            go!: /showRank
        else:
            go!: /errorNode
    
    state: showRank
        script:
            var $session = $jsapi.context().session;
            var texts = $session.response.data.result.texts
            var reply = {"type":"text", "text":texts};
            $response.replies = $response.replies || [];
            $response.replies.push(reply);
            
            var tables = $session.response.data.result.tables
            
            var cells = [];
            for (var t in tables) {
                var table = tables[t]
                for (var row in table) {
                    cells.push({
                        "title":row,
                        "value":table[row],
                        "subtitle":""
                    });
                }
            }
            var reply = {
                "type": "cardList",
                "title": "Сравнение прогноза с планом до конца года",
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