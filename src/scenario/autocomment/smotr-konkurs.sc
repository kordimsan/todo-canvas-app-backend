theme: /
    state: getSK
        script:
            var params = {
                "id": 8,
                "tb": $session.tb,
                "year": parseInt($session.year),
                "month": parseInt($session.month)
            };
            $session.response = getData(params);
            
        if: $session.response.status == 200
            go!: /showSK
        else:
            go!: /errorNode
    
    state: showSK
        script:
            var $session = $jsapi.context().session;
            var texts = $session.response.data.result.texts
            var reply = {"type":"text", "text":texts};
            $response.replies = $response.replies || [];
            $response.replies.push(reply);
            
            var tables = $session.response.data.result.tables
            
            var cells = [];
            
            var table = tables[0]
            var table0 = tables[4]
            for (var row in table) {
                cells.push({
                    "title":row,
                    "value":table[row],
                    "subtitle":table0[row]
                });
            }
            
            var reply = {
                "type": "cardList",
                "title": "Место ТБ по показателям в конкурсе территорий",
                "subtitle": "Объект анализа: " + table["Подразделение"],
                "cells": [
                    {
                        "title": "Операционная прибыль",
                        "value": table["Операционная прибыль"],
                        "subtitle": "Изменение к пред. кварталу: " + (table["Операционная прибыль"] - table0["Операционная прибыль"]) + " места"
                    },
                    {
                        "title": "CIR",
                        "value": table["CIR"],
                        "subtitle": "Изменение к пред. кварталу: " + (table["CIR"] - table0["CIR"]) + " места"
                    },
                    {
                        "title": "СберПрайм",
                        "value": table["СберПрайм"],
                        "subtitle": "Изменение к пред. кварталу: " + (table["СберПрайм"] - table0["СберПрайм"]) + " места"
                    },
                    {
                        "title": "Охват небанковскими сервисами",
                        "value": table["Охват небанковскими сервисами"],
                        "subtitle": "Изменение к пред. кварталу: " + (table["Охват небанковскими сервисами"] - table0["Охват небанковскими сервисами"]) + " места"
                    },
                    {
                        "title": "Ранг",
                        "value": table["Ранг"],
                        "subtitle": "Изменение к пред. кварталу: " + (table["Ранг"] - table0["Ранг"]) + " места"
                    },
                ],
                "auto_listening": false
            };
            $response.replies = $response.replies || [];
            $response.replies.push(reply);
            
        buttons:
            "Назад" -> /getPreview
        buttons:
            "Сравнить с ТОП-1" -> /showSKTop1
        buttons:
            "Сравнить с ТОП-3" -> /showSKTop3
        buttons:
            "Завершить диалог" -> /finish
        

    state: showSKTop1
        script:
            var $session = $jsapi.context().session;
            var tables = $session.response.data.result.tables
            
            var table = tables[0]
            var table0 = tables[1]
            var reply = {
                "type": "cardList",
                "title": "Место ТБ по показателям в конкурсе территорий в ставнении с ТОП 1",
                "subtitle": "Объект анализа: " + table["Подразделение"],
                "cells": [
                    {
                        "title": "Операционная прибыль",
                        "value": table["Операционная прибыль"],
                        "subtitle": table0["Подразделение"] + ": " + table0["Операционная прибыль"]
                    },
                    {
                        "title": "CIR",
                        "value": table["CIR"],
                        "subtitle": table0["Подразделение"] + ": " + table0["CIR"]
                    },
                    {
                        "title": "СберПрайм",
                        "value": table["СберПрайм"],
                        "subtitle": table0["Подразделение"] + ": " + table0["СберПрайм"]
                    },
                    {
                        "title": "Охват небанковскими сервисами",
                        "value": table["Охват небанковскими сервисами"],
                        "subtitle": table0["Подразделение"] + ": " + table0["Охват небанковскими сервисами"]
                    },
                    {
                        "title": "Ранг",
                        "value": table["Ранг"],
                        "subtitle": table0["Подразделение"] + ": " + table0["Ранг"]
                    },
                ],
                "auto_listening": false
            };
            $response.replies = $response.replies || [];
            $response.replies.push(reply);
            
        buttons:
            "Назад" -> /showSK
        buttons:
            "Сравнить с ТОП-3" -> /showSKTop3
        buttons:
            "Завершить диалог" -> /finish
    
    
    state: showSKTop3
        script:
            var $session = $jsapi.context().session;
            var tables = $session.response.data.result.tables
            
            var table = tables[0]
            var table1 = tables[1]
            var table2 = tables[2]
            var table3 = tables[3]
            var reply = {
                "type": "cardList",
                "title": "Место ТБ по показателям в конкурсе территорий в ставнении с ТОП 3",
                "subtitle": "Объект анализа: " + table["Подразделение"],
                "cells": [
                    {
                        "title": "Операционная прибыль",
                        "value": table["Операционная прибыль"],
                        "subtitle": table1["Подразделение"] + ": " + table1["Операционная прибыль"] + ", " + table2["Подразделение"] + ": " + table2["Операционная прибыль"] + ", " + table3["Подразделение"] + ": " + table3["Операционная прибыль"]
                    },
                    {
                        "title": "CIR",
                        "value": table["CIR"],
                        "subtitle": table1["Подразделение"] + ": " + table1["CIR"] + ", " + table2["Подразделение"] + ": " + table2["CIR"] + ", " + table3["Подразделение"] + ": " + table3["CIR"]
                    },
                    {
                        "title": "СберПрайм",
                        "value": table["СберПрайм"],
                        "subtitle": table1["Подразделение"] + ": " + table1["СберПрайм"] + ", " + table2["Подразделение"] + ": " + table2["СберПрайм"] + ", " + table3["Подразделение"] + ": " + table3["СберПрайм"]
                    },
                    {
                        "title": "Охват небанковскими сервисами",
                        "value": table["Охват небанковскими сервисами"],
                        "subtitle": table1["Подразделение"] + ": " + table1["Охват небанковскими сервисами"] + ", " + table2["Подразделение"] + ": " + table2["Охват небанковскими сервисами"] + ", " + table3["Подразделение"] + ": " + table3["Охват небанковскими сервисами"]
                    },
                    {
                        "title": "Ранг",
                        "value": table["Ранг"],
                        "subtitle": table1["Подразделение"] + ": " + table1["Ранг"] + ", " + table2["Подразделение"] + ": " + table2["Ранг"] + ", " + table3["Подразделение"] + ": " + table3["Ранг"]
                    },
                ],
                "auto_listening": false
            };
            $response.replies = $response.replies || [];
            $response.replies.push(reply);
            
        buttons:
            "Назад" -> /showSK
        buttons:
            "Сравнить с ТОП-1" -> /showSKTop1
        buttons:
            "Завершить диалог" -> /finish