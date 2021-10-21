theme: /
    state: getPreview
        script:
            var params = {
                "id": 1,
                "tb": $session.tb,
                "year": parseInt($session.year),
                "month": parseInt($session.month)
            };
            $session.response = getData(params);
            
        if: $session.response.status == 200
            go!: /showPreview
        else:
            go!: /errorNode
        
    state: showPreview
        script:
            var $session = $jsapi.context().session;
            var texts = $session.response.data.result.texts;
            $response.replies = $response.replies || [];
            var reply = {"type":"text", "text":texts.join("\n")};
            $response.replies.push(reply);

        script:
            var month_text = {
                "01": "Январь",
                "02": "Февраль",
                "03": "Март",
                "04": "Апрель",
                "05": "Май",
                "06": "Июнь",
                "07": "Июль",
                "08": "Август",
                "09": "Сентябрь",
                "10": "Октябрь",
                "11": "Ноябрь",
                "12": "Декабрь",
            }
        
            var $session = $jsapi.context().session;
            var tables = $session.response.data.result.tables
            $response.replies = $response.replies || [];
            //for (var i in tables) {
            var table = tables[0]
            var res = []
            for (var row in table) {
                
                var val = table[row]
                
                res.push({
                "title": row,
                "value": val[0],
                "subtitle": val[1] == "" ? "" : "План: " + val[1],
                "iconUrl":"","hash":"","action":{"name": row}});
            }
            
            var reply = {
              "type": "cardList",
              "title": "Показатели ТБ Байкальский банк",
              "subtitle": "Период: Январь-" + month_text[$session.month] + " " + $session.year,
              "cells": res,
              "auto_listening": false
            };
            $response.replies.push(reply);
            //}

        
        buttons:
            "CIR по блокам" -> /getCir/0
        buttons:
            "CIR по ГОСБ" -> /getCir/2
        buttons:
            "Сравнить факт с аналог. периодом прошлого года" -> /detailsCir1
        buttons:
            "Сравнить прогноз с планом до конца года" -> /detailsCir2
        buttons:
            "Что с рангом" -> /getSK
        buttons:
            "Ранг по CIR"
        buttons:
            "Подробнее о расходах"
        buttons:
            "Поменять период" -> /getPeriod
        buttons:
            "Завершить диалог" -> /finish
    
    state: detailsCir1
        script:
            var month_text = {
                "01": "Январь",
                "02": "Февраль",
                "03": "Март",
                "04": "Апрель",
                "05": "Май",
                "06": "Июнь",
                "07": "Июль",
                "08": "Август",
                "09": "Сентябрь",
                "10": "Октябрь",
                "11": "Ноябрь",
                "12": "Декабрь",
            }
        
            var $session = $jsapi.context().session;
            var tables = $session.response.data.result.tables
            $response.replies = $response.replies || [];
            //for (var i in tables) {
            var table = tables[1]
            var res = []
            for (var row in table) {
                
                var val = table[row]
                
                res.push({
                "title": row,
                "value": val[0],
                "subtitle": val[1] == "" ? "" : "Факт прошлого года: " + val[1],
                "iconUrl":"","hash":"","action":{"name": row}});
            }
            
            var reply = {
              "type": "cardList",
              "title": "Сравнение факта текущего периода с аналогичным периодом прошлого года",
              "subtitle": "Период: Январь-" + month_text[$session.month] + " " + $session.year,
              "cells": res,
              "auto_listening": false
            };
            $response.replies.push(reply);
            
        buttons:
            "Назад" -> /showPreview
        buttons:
            "Завершить диалог" -> /finish
    
    state: detailsCir2
        script:
            var month_text = {
                "01": "Январь",
                "02": "Февраль",
                "03": "Март",
                "04": "Апрель",
                "05": "Май",
                "06": "Июнь",
                "07": "Июль",
                "08": "Август",
                "09": "Сентябрь",
                "10": "Октябрь",
                "11": "Ноябрь",
                "12": "Декабрь",
            }
        
            var $session = $jsapi.context().session;
            var tables = $session.response.data.result.tables
            $response.replies = $response.replies || [];
            //for (var i in tables) {
            var table = tables[2]
            var res = []
            for (var row in table) {
                
                var val = table[row]
                
                res.push({
                "title": row,
                "value": val[0],
                "subtitle": val[1] == "" ? "" : "План: " + val[1],
                "iconUrl":"","hash":"","action":{"name": row}});
            }
            
            var reply = {
              "type": "cardList",
              "title": "Сравнить прогноз с планом до конца года",
              "subtitle": "Период: Январь-" + month_text[$session.month] + " " + $session.year,
              "cells": res,
              "auto_listening": false
            };
            $response.replies.push(reply);
            
        buttons:
            "Назад" -> /showPreview
        buttons:
            "Завершить диалог" -> /finish