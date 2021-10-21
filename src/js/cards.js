function pushAnswerWithListCard(cellsList, buttonsList, caption){
    $jsapi.context().response.replies = $jsapi.context().response.replies || [];
    var card = {
        "type": "cardList",
        "title": caption,
        "subtitle": "",
        "cells": cellsList,
        "buttons": buttonsList,
        "auto_listening": false
    };
    $jsapi.log("Карточка ответа:\n" + JSON.stringify(card));
    $jsapi.context().response.replies.push(card);
}

function pushAnswerWithListCard_raw(cellsList, text, pronounceText){
    $jsapi.context().response.replies = $jsapi.context().response.replies || [];
    var card = {
        "type": "raw",
        "body": { 
            "items":[
                {
                    "card": {
                        "type": "list_card",
                        "paddings": {
                            "top": "9x",
                            "bottom": "12x",
                            "left": "8x",
                            "right": "8x"
                        },
                        "cells": cellsList
                    }
                },
                {
                    "bubble": {
                        "text": text,
                        "markdown": true
                    }
                },
            ],
            "pronounceText": pronounceText
        },
        "messageName": "ANSWER_TO_USER"
    };
    $jsapi.log("Карточка ответа: " + card);
    $jsapi.context().response.replies.push(card);
}

function createCell(text, caption) {
    var cell = {
        "title": caption,
        "subtitle": "",
        "value": text,
        "action": {
            "text": text
        }
    };
    return cell
}

function createCell_raw(text) {
    var cell = {
        "type": "text_cell_view",
        "content": {
            "text": text,
            "typeface": "headline1",
            "text_color": "default",
            "max_lines": 0
        },
        "paddings": {
            "left": "8x",
            "top": "10x",
            "right": "8x"
        }
    };
    return cell
}