theme: /

    state: getCir
        q!: (~показать|покажи)
            $AnyText::anyText
        
        script:
            var command = {
                "type": "smart_app_data",
                "smart_app_data": {
                    "text": "План по CIR за 6 мес. - <font color='green'>выполнен</font>.\n\nВот основные результаты:\n\n- Фактический CIR - <font color='red'>13,6%</font>, что лучше среднего значения по всем тербанкам - 14,9%\n- Расходы ниже плана (<font color='green'>-11,4%</font>)на фоне одновременного превышенияплана по доходам (+8%)\n- Прогнозируемый CIR находитсяв пределах плана до конца года",
                    "tableTitle": "Влияние ГОСБ на CIR ТБ",
                    "tableSubtitle": "Период: Январь-Июнь 2021",
                    "tableHeaders": ["Название", "Факт", "План", "Прогноз", "Влияние"],
                    "tableDataNames": [["РБ+БП+УСБ"], ["КИБ"], ["УБ"], ["БСП"]],
                    "tableData": [
                        [
                            ["20", "%", "#FA6D20"],
                            ["2000", "тыс.руб.", "#24B23E"],
                            ["1", "", "#FA6D20"],
                            ["-10", "%", "#FA6D20"],
                        ],
                        [
                            ["100", "%", "#24B23E"],
                            ["-200", "тыс.руб.", "#FFFFFF"],
                            ["2", "", "#24B23E"],
                            ["15", "%", "#FA6D20"],
                        ],
                        [
                            ["-100", "%", "#FA6D20"],
                            ["1,1", "тыс.руб.", "#FFFFFF"],
                            ["12.3", "", "#FA6D20"],
                            ["12,2", "%", "#FFFFFF"],
                        ],
                        [
                            ["1", "%", "#FFFFFF"],
                            ["321 312", "тыс.руб.", "#FA6D20"],
                            ["4", "", "#24B23E"],
                            ["33", "%", "#FFFFFF"],
                        ],
                    ]
                }
            };

            var emotion = "udovolstvie";
            var body = {emotion: emotion, items: [{command: command}]};
            
            var replyData = {
                type: "raw",
                body: body
            };    
            $context.response.replies = $context.response.replies || [];
            $context.response.replies.push(replyData);
            
        a: Вот!