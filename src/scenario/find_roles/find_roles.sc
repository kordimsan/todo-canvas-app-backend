theme: /
    state: inline_slots_filling
        intent!: /find_roles
        go!: /profile_request
        
    # запрос к профилю пользователя
    state: profile_request
        script:
            // ИФТ стенд
            $session.profile_response = get_request_uvhd("/sap/opu/odata/SAP/ZSB_D7915_PA_UTILS_SRV/EmployeeSet(UserId='19115',Pernr='19115')?$format=json&$select=PositTxt,Orgeh,Bukrs,Kostl,Fistl,Block", {}, "IFT");
        if: $session.profile_response.status == 200
            go!: /get_role_list
        else:
            go!: /profile_error
        
    state: profile_error
        a: При выполнении запроса к профилю пользователя возникла ошибка {{JSON.stringify($session.profile_response)}}
        go!: /end
        
    state: get_role_list
        script:
            var data = {
                "ЦЗ": $session.profile_response.data.d.Fistl, 
                "Должность": $session.profile_response.data.d.PositTxt, 
                "БЕ": $session.profile_response.data.d.Bukrs,
                "text": $request.query
            };
            # Запрос к ИФТ стенду MWS
            #$session.role_list_response = post_request_mws("https://dfsa-roles-1-10000.ci00324559-dev-mws.apps.test-ose.ca.sbrf.ru/dfsa-roles/predict", {"Data": data, "Text": $request.query});
            # Запрос к dev стенду MWS
            #$session.role_list_response = post_request_mws("http://10.116.150.42:9934/getRoleList", {"Data": $request.query});
            $session.role_list_response = post_request_mws("https://dfsa-get-role-list-1-10000.ci00324559-dev-mws.apps.test-ose.ca.sbrf.ru/dfsa-get-role-list/predict", {"Data": data});
        if: $session.role_list_response.status == 200
            go!: /get_roles_from_sap_request
        else:
            go!: /role_list_error
    
    state: get_roles_from_sap_request
        script:
            var roles_found = $session.role_list_response.data.result;
            $session.roles_order = {};
            var role_codes = [];
            var decimal_role_codes = [];
            // генерация параметра с номерами ролей для запроса
            var roles_param = "(";
            roles_found.forEach(function(item, i, roles_found){
                role_codes.push(item.role_number);
                decimal_role_codes.push(parseInt(item.role_number, 10));
                var or_txt = "";
                if (i < roles_found.length - 1) { or_txt = "%20or%20"; }
                roles_param = roles_param + "AgrNum%20eq%20%27" + item.role_number + "%27" + or_txt;
                // заполняем маппинг код роли -> ранг
                $session.roles_order[parseInt(item.role_number, 10)] = item.rank;
            });
            $jsapi.log("==MWS ROLES== " + JSON.stringify(decimal_role_codes));
            if (decimal_role_codes.length == 0) {
                $reactions.transition("/no_recommended_roles");
            }
            $session.mws_role_codes = decimal_role_codes;
            roles_param = roles_param + ")";
            // запрос в сервис getRoles (dev стенд)
            var url = "/sap/opu/odata/SAP/ZSB_D8070_ROLES_SRV/RoleSet?$format=json&$filter=UserId%20eq%20%2719115%27%20and%20Pernr%20eq%20%2719115%27%20and%20AgrSyst%20eq%20%2701%27%20and%20AgrInstance%20eq%20%2701%27%20and%20" + roles_param + "&$expand=toParamDef,toParamVal&$select=AgrNum,AgrTitle,IsAssigned,toParamDef/*,toParamVal/*";
            $session.get_roles_from_sap_response = get_request_uvhd(url, {}, "DEV");
        if: $session.get_roles_from_sap_response.status == 200
            go!: /process_sap_roles
        else:
            go!: /get_roles_from_sap_error
        
    state: process_sap_roles
        script:
            var sap_roles = $session.get_roles_from_sap_response.data.d.results;
            var needed_roles_found = [];
            var already_has_roles = [];
            var print_txt = "";
            // Проверяем, какие роли уже присвоены пользователю
            sap_roles.forEach(function(element){
                // если роль присвоена
                $jsapi.log("==ROLE== " + JSON.stringify(element.AgrNum) + " " + element.IsAssigned);
                var role_is_assigned = $session.mws_role_codes.filter(function(sap_role){
                    return (sap_role == parseInt(element.AgrNum, 10)) && element.IsAssigned == false;
                });
                if (role_is_assigned.length > 0) { needed_roles_found.push(element); }
                // если роль не присвоена
                var role_is_not_assigned = $session.mws_role_codes.filter(function(sap_role){
                    return (sap_role == parseInt(element.AgrNum, 10)) && element.IsAssigned == true;
                });
                if (role_is_not_assigned.length > 0) { already_has_roles.push(element); }
                element.order = 1;
            });
            if (already_has_roles.length > 0) {
                $session.already_has_roles = already_has_roles;
                $reactions.transition("/user_has_role");
            }
            else {
                // сортируем роли по рангу
                sortRoles(needed_roles_found, $session.roles_order);
                $session.final_roles = needed_roles_found;
                $reactions.transition("/show_roles");
            }
        
    state: user_has_role
        script:
            if ($session.already_has_roles.length == 1) {
                $reactions.transition("user_has_role_analytics");
            }
            else {
                var max_analytics = 4;
                var cells_detailed = [];
                var cells_combined = [];
                var valid_choices_detailed = [];
                var valid_choices_combined = [];
                var roles = $session.already_has_roles;
                var is_analytics_limit_exceeded = false;
                
                roles.forEach(function(role){
                    var role_analytics_mapping = {};
                    // Создаем маппинг код аналитики->название аналитики
                    role.toParamDef.results.forEach(function(analytic){
                        role_analytics_mapping[analytic.AgrParam] = analytic.ParName;
                    });
                    var role_param_val = role.toParamVal.results;
                    var analytics_groups = {};
                    // Формируем список групп для аналитик
                    role_param_val.forEach(function(analytic) {
                        // создаем структуру для новой аналитики
                        if (!((analytic.AgrParam) in analytics_groups)) {
                            analytics_groups[analytic.AgrParam] = [];
                        }
                        // добавляем новое значение для аналитики в список
                        analytics_groups[analytic.AgrParam].push(analytic.ParValue);
                    });
                    
                    // проверяем, есть ли превышение лимита выводимых значений аналитик
                    var analytics_count = 0;
                    Object.keys(analytics_groups).forEach(function(analytic){
                        var analytic_values = analytics_groups[analytic];
                        analytics_count = analytics_count + analytic_values.length;
                        if (analytics_count > max_analytics) {
                            is_analytics_limit_exceeded = true;
                            return;
                        }
                    });
                    var all_analytics_txt_detailed = "";
                    var all_analytics_txt_combined = "";
                    // Формирование текста для строк карточки
                    Object.keys(analytics_groups).forEach(function(analytic){
                        var analytic_values = analytics_groups[analytic];
                        // формируем строку вида - <аналитика>: <количество значений>, ...
                        //                   либо - <аналитика>: <значение>, <значение> <аналитика>: <значение>
                        all_analytics_txt_detailed = all_analytics_txt_detailed + role_analytics_mapping[analytic] + ": " + analytic_values.join(" ") + ", ";
                        all_analytics_txt_combined = all_analytics_txt_combined + analytic_values.length + " " + role_analytics_mapping[analytic] + ", ";
                    });
                    // убираем ", " из конца строки
                    all_analytics_txt_detailed = all_analytics_txt_detailed.slice(0, -2);
                    all_analytics_txt_combined = all_analytics_txt_combined.slice(0, -2);
                    
                    all_analytics_txt_detailed = parseInt(role.AgrNum) + " " + role.AgrTitle + " (" + all_analytics_txt_detailed + ")";
                    all_analytics_txt_combined = parseInt(role.AgrNum) + " " + role.AgrTitle + " (" + all_analytics_txt_combined + ")";
                    cells_detailed.push(createCell(all_analytics_txt_detailed, "Нажмите, чтобы развернуть"));
                    cells_combined.push(createCell(all_analytics_txt_combined, "Нажмите, чтобы развернуть"));
                    // формируем список валидного текста для последующего парсинга
                    valid_choices_detailed.push(all_analytics_txt_detailed);
                    valid_choices_combined.push(all_analytics_txt_combined);
                });
                $session.has_multiple_role_choices = true;
                var role_txt = "У вас уже присвоены роли:";
                // выбираем, какой тип ответа выдать - свернутый или полный
                if (is_analytics_limit_exceeded == true || analytics_groups["BUKRS"].length > 1) {
                    $session.valid_role_choices = valid_choices_combined;
                    pushAnswerWithListCard(cells_combined, [], role_txt);
                }
                else {
                    $session.valid_role_choices = valid_choices_detailed;
                    pushAnswerWithListCard(cells_detailed, [], role_txt);
                }
            }
            
        state: user_has_role_analytics
            q: $regexp<\d+ \W+.*>
            script:
                var query;
                // если вернулись по кнопке назад
                if ("was_in_analytics_detailed" in $session) {
                    query = $session.backup_role_analytics_query;
                    delete $session.was_in_analytics_detailed;
                }
                else {
                    $jsapi.log("==QUERY== " + $request.query);
                    $session.backup_role_analytics_query = $request.query;
                    query = $request.query;
                }
                var parsed_role;
                if ($session.has_multiple_role_choices == true && $session.valid_role_choices.indexOf(query) != -1) {
                    var result = /(\d+).*/.exec(query);
                    if (result.length > 1) { parsed_role = result[1]; }
                }
                var max_analytics = 3;
                var cells = [];
                var available_analytics = [];
                var roles = $session.already_has_roles;
                var role;
                // если роль была выбрана из карточки, находим ее
                if (parsed_role != undefined) {
                    roles.forEach(function(item){
                        if (parseInt(item.AgrNum, 10) == parseInt(parsed_role, 10)) {
                            role = item;
                            return;
                        }
                    });
                }
                // если нет, значит изначально была только одна роль
                else {
                    role = roles[0];
                }
                $session.user_role = role;
                var use_analytics_small_model = false;
                var role_analytics_mapping = {};
                // Создаем маппинг код аналитики->название аналитики
                role.toParamDef.results.forEach(function(analytic){
                    role_analytics_mapping[analytic.AgrParam] = analytic.ParName;
                });
                var role_param_val = role.toParamVal.results;
                var analytics_groups = {};
                var analytics_info_detailed = {};
                // Формируем список групп по БЕ для аналитик
                var has_profile_kostl = false;
                var has_profile_objid = false;
                role_param_val.forEach(function(analytic){
                    // сверяем МВЗ присвоенной роли со значением МВЗ в профиле
                    if (analytic.AgrParam == "KOSTL" && has_profile_kostl == false) {
                        if (analytic.ParValue == $session.profile_response.data.d.Kostl) {
                            has_profile_kostl = true;
                        }
                    }
                    // сверяем ОЕ присвоенной роли со значением ОЕ в профиле
                    if (analytic.AgrParam == "OBJID" && has_profile_objid == false) {
                        if (analytic.ParValue == $session.profile_response.data.d.Orgeh) {
                            has_profile_objid == true;
                        }
                    }
                    if (analytic.AgrParam == "BUKRS") {
                        analytics_groups[analytic.ParValue] = {};
                        analytics_info_detailed[analytic.ParValue] = [];
                    }
                });
                // если МВЗ или ОЕ совпадают по значению с профилем, используем простую модель
                if (has_profile_kostl == true || has_profile_objid == true) {
                    use_analytics_small_model = true;
                }
                $session.use_analytics_small_model = use_analytics_small_model;
        
                // создание и обход графов
                var graph_txt = {};
                var be_graphs = createGraphsForRole(role);
                Object.keys(be_graphs).forEach(function(be){
                    var graph = be_graphs[be];
                    var start = "БЕ " + be;
                    
                    var txt = graphDfs(graph, start, true);
                    graph_txt[be] = txt;
                });
                $session.graph_txt = graph_txt;
                
                var analytics_txt = "";
                // Формируем группы аналитик по БЕ
                role_param_val.forEach(function(analytic) {
                    // добавляем новое значение аналитики в группу
                    if (analytic.AgrParam != "BUKRS") {
                       // $jsapi.log("==ANALYTIC== " + analytic.AgrParam + " " + analytic.ParValue + " " + analytic.ParentAgrParam + " " + analytic.ParentParValue);
                        // создаем структуру для новой аналитики
                        if (!((analytic.AgrParam) in analytics_groups[analytic.Bukrs])) {
                            analytics_groups[analytic.Bukrs][analytic.AgrParam] = [];
                        }
                        // добавляем новое значение для аналитики в список
                        analytics_groups[analytic.Bukrs][analytic.AgrParam].push(analytic.ParValue);
                    }
                });
                // проверяем, есть ли БЕ с превышением лимита выводимых значений аналитик
                var is_analytics_limit_exceeded = false;
                // loop for BE
                Object.keys(analytics_groups).forEach(function(be_value){
                    var be_analytics = analytics_groups[be_value];
                    var analytics_count = 0;
                    // если лимит уже превышен количеством аналитик, то можно не считать количество значений
                    if (be_analytics.length > max_analytics) {
                        is_analytics_limit_exceeded = true;
                        return;
                    }
                    // loop for analytics in BE
                    Object.keys(be_analytics).forEach(function(analytic_name){
                        analytics_count = analytics_count + be_analytics[analytic_name].length;
                        if ((be_analytics[analytic_name].length > max_analytics) || (analytics_count > max_analytics)) {
                            is_analytics_limit_exceeded = true;
                            return;
                        }
                    });
                });
                
                Object.keys(analytics_groups).forEach(function(be_value){
                    var all_analytics_txt = "";
                    var be_analytics = analytics_groups[be_value];
                    // формируем строку вида - <аналитика>: <количество значений>, ...
                    //                   либо - <аналитика>: <значение>, <значение> <аналитика>: <значение>
                    Object.keys(be_analytics).forEach(function(analytic_name){var analytics_info = "";
                        // если количество аналитик превышает установленный лимит - сворачиваем тексты
                        if (is_analytics_limit_exceeded == true) {
                            analytics_info = be_analytics[analytic_name].length + " " + role_analytics_mapping[analytic_name] + ", ";
                            analytics_info_detailed[be_value].push(role_analytics_mapping[analytic_name] + ": " + be_analytics[analytic_name].join(" ") + "<br>");
                        }
                        else {
                            analytics_info = role_analytics_mapping[analytic_name] + ": " + be_analytics[analytic_name].join(" ") + ", ";
                                analytics_info_detailed[be_value].push(role_analytics_mapping[analytic_name] + ": " + be_analytics[analytic_name].join(" ") + "<br>");
                        }
                        all_analytics_txt = all_analytics_txt + analytics_info;
                    });
                    // убираем ", " из конца строки
                    all_analytics_txt = all_analytics_txt.slice(0, -2);
                    // формируем строку вида - БЕ: <значение БЕ>, <список аналитик><br>
                    all_analytics_txt = role_analytics_mapping["BUKRS"] + ": " + be_value + ", " + all_analytics_txt;
                    var cell_text = all_analytics_txt;
                    available_analytics.push(cell_text);
                    cells.push(createCell(cell_text, "Нажмите, чтобы развернуть"));
                });
                $session.available_analytics = available_analytics;
                $session.analytics_info_detailed = analytics_info_detailed;
                var role_txt = "У вас уже присвоена роль " + parseInt(role.AgrNum) + " " + role.AgrTitle;
                pushAnswerWithListCard(cells, [], role_txt);
                if (parsed_role != undefined) {
                    $reactions.buttons([
                        { text: "Нужны другие аналитики",
                          transition: "./user_has_role_show_analytic_groups" },
                        { text: "Назад", transition: ".." }, 
                        { text: "Завершить", transition: "/end" }
                    ]);
                }
                else {
                    $reactions.buttons([
                        { text: "Нужны другие аналитики",
                          transition: "./user_has_role_show_analytic_groups" }, 
                        { text: "Завершить", transition: "/end" }
                    ]);
                }
            state: user_has_role_show_analytic_groups
                script:
                    var data = {
                        "role": $session.user_role.AgrNum,
                        "be": $session.profile_response.data.d.Bukrs,
                        "oe": $session.profile_response.data.d.Orgeh,
                        "oe2": "10237908",
                        "top": 3
                    }
                    if ($session.use_analytics_small_model) {
                        $session.role_analytics_mws_response = post_request_mws("https://dfsa-get-role-analytics-small-1-10000.ci00324559-dev-mws.apps.test-ose.ca.sbrf.ru/dfsa-get-role-analytics-small/predict", {"Data": data});
                    }
                    else {
                        $session.role_analytics_mws_response = post_request_mws("https://dfsa-get-role-analytics-1-10000.ci00324559-dev-mws.apps.test-ose.ca.sbrf.ru/dfsa-get-role-analytics/predict", {"Data": data});
                    }
                    if ($session.role_analytics_mws_response.status != 200) {
                        $reactions.transition("/role_analytics_mws_error");
                    }
                    else {
                        var mws_analytics = $session.role_analytics_mws_response.data;
                    
                        if (mws_analytics.length < 1) {
                            $reactions.transition("./user_has_role_show_analytics");
                        }
                        else {
                            // показ наборов аналитик
                            var chosen_role = $session.user_role.toParamDef.results;
                            var role_analytics_mapping = {};
                            // Создаем маппинг код аналитики->название аналитики
                            chosen_role.forEach(function(analytic){
                                role_analytics_mapping[analytic.AgrParam.toLowerCase()] = analytic.ParName;
                            });
                            var cells = [];
                            mws_analytics.forEach(function(analytic_group) {
                                var cell_text = "";
                                Object.keys(analytic_group).forEach(function(analytic) {
                                    if (analytic != "role") {
                                        cell_text = cell_text + role_analytics_mapping[analytic.toLowerCase()] + ": " + analytic_group[analytic] + ", ";
                                    }
                                });
                                cell_text = cell_text.slice(0, -2);
                                cells.push(createCell(cell_text, ""));
                            });
                            cells.push(createCell("Нет подходящего варианта", ""));
                            pushAnswerWithListCard(
                                cells, 
                                [], 
                                "Выберите подходящую комбинацию аналитик"
                            );
                        }
                    }
                
                state: user_has_role_show_analytics
                    q: $regexp<([А-я ]+: [А-яA-z0-9- ]+,?)+>
                    q: Нет подходящего варианта
                    script:
                        var query;
                        // если вернулись из ввода аналитик
                        if ("was_in_analytics_input" in $session) {
                            query = $session.backup_show_role_analytics_query;
                            delete $session.was_in_analytics_input;
                        }
                        else {
                            $session.backup_show_role_analytics_query = $request.query;
                            query = $request.query;
                        }
                    
                        var chosen_analytics_group = [];
                        var result = /([А-я ]+: [А-яA-z0-9- ]+[,]?)+/i.exec(query);
                        if (result != undefined) {
                            chosen_analytics_group = result[0].split(", ");
                        }
                        var mws_analytics = {};
                        // если был переход из выбора группы аналитик, парсим группу
                        if (chosen_analytics_group.length > 0) {
                            chosen_analytics_group.forEach(function(analytic) {
                                var analytic_info = analytic.split(": ");
                                mws_analytics[analytic_info[0].toLowerCase()] = analytic_info[1];
                            });
                        }
                        // если был прямой переход, парсим данные из модели
                        else {
                            if ($session.role_analytics_mws_response.data.length > 0) {
                                var group = $session.role_analytics_mws_response.data[0];
                                Object.keys(group).forEach(function(analytic) {
                                    if (analytic != "role") {
                                        mws_analytics[analytic.toLowerCase()] = group[analytic];
                                    }
                                });
                            }
                        }
                        var analytics_mapping = {
                            #"BUKRS":  $session.profile_response.data.d.Bukrs,
                            #"BLK":    $session.profile_response.data.d.Block,
                            "KOSTL":  $session.profile_response.data.d.Kostl,
                            "OBJID":  $session.profile_response.data.d.Orgeh,
                            #"ZFICTR": $session.profile_response.data.d.Fistl
                        };
                        var chosen_role = $session.user_role.toParamDef.results;
                        var role_analytics_mapping = {};
                        // Создаем маппинг код аналитики->название аналитики
                        chosen_role.forEach(function(analytic){
                            role_analytics_mapping[analytic.AgrParam.toLowerCase()] = analytic.ParName.toLowerCase();
                        });
                        var role_analytics = $session.user_role.toParamDef.results;
                        var analytics_for_input = [];
                        var cells = [];
                        role_analytics.forEach(function(analytic, i, role_analytics){
                            var analytic_value;
                            var analytic_real_name = role_analytics_mapping[analytic.AgrParam.toLowerCase()];
                            #var analytic_real_name = analytic.AgrParam.toLowerCase();
                            // если значение аналитики пришло из модели
                            if (analytic_real_name in mws_analytics) {
                                analytic_value = mws_analytics[analytic_real_name];
                            }
                            // по дефолту берем значение из профиля пользователя
                            else {
                                analytic_value = analytics_mapping[analytic.AgrParam];
                            }
                            // если значение аналитики было введено пользователем
                            if ($session.analytics_input_values != undefined) {
                                if ((analytic.ParName) in $session.analytics_input_values) {
                                    analytic_value = $session.analytics_input_values[analytic.ParName];
                                }
                            };
                            if (analytic_value == undefined) {
                                analytics_for_input.push(analytic);
                            }
                            
                            cells.push(createCell(analytic.ParName + ((analytic_value == undefined) ? "" : ": " + analytic_value), "Изменить"));
                        });
                        $session.analytics_for_input = analytics_for_input;
                        if ($session.analytics_for_input.length > 0) {
                            $reactions.transition("./input_analytics");
                        }
                        else {
                            var header = "Аналитики роли " + parseInt($session.user_role.AgrNum, 10) +  " " + $session.user_role.AgrTitle;
                            pushAnswerWithListCard(cells, [], header);
                            $reactions.buttons([
                                { text: "Создать заявку", transition: "/create_role_task" },
                                { text: "Завершить", transition: "/end" }
                            ]);
                        }
                    
                    state: input_analytics
                        q: $regexp<[А-я ]+: [А-яA-z0-9]+>
                        script:
                            var analytic_for_input;
                            var result = /([А-я ]+: [А-яA-z0-9]+)/i.exec($request.query);
                            if (result != undefined) {
                                analytic_for_input = result[0];
                                analytic_for_input = analytic_for_input.split(": ");
                            }
                            if ($session.analytics_input_values == undefined) {
                                $session.analytics_input_values = {};
                            }
                            var analytics_for_delete = [];
                            var has_message_for_delete = false;
                            
                            if ($session.has_graph == undefined && analytic_for_input != undefined) {
                                // создание и обход графов
                                if ($session.analytics_for_input == undefined) {
                                    $session.analytics_for_input = [];
                                }
                                var role = $session.user_role;
                                var graph = createSingleGraphForRole(role);
                                var start = analytic_for_input[0];
                                $session.chosen_analytic_for_delete = analytic_for_input[0];
                                analytics_for_delete = graphDfs(graph, start);
                                var role_analytics = role.toParamDef.results;
                                role_analytics.forEach(function(analytic, i, role_analytics){
                                    var to_delete = analytics_for_delete.filter(function(a){
                                        return a == analytic.ParName;
                                    });
                                    if (to_delete.length > 0) {
                                        $session.analytics_for_input.push(analytic);
                                        has_message_for_delete = true;
                                    }
                                });
                            }
                            if (has_message_for_delete == true && $session.analytics_for_input.length > 1) {
                                $reactions.answer("Изменение аналитики {{$session.chosen_analytic_for_delete}} требует изменения всех зависимых от нее аналитик");
                                has_message_for_delete = false;
                                delete $session.chosen_analytic_for_delete;
                            }
                            $session.was_in_analytics_input = true;
                            // если введены не все аналитики
                            if ($session.analytics_for_input.length > 0 || analytic_for_input != undefined) {
                                if ($session.analytics_for_input.length > 0) {
                                    var role_analytic = $session.analytics_for_input.shift();
                                    $session.current_input_analytic = role_analytic.ParName;
                                }
                                else if (analytic_for_input != undefined) {
                                    $session.current_input_analytic = analytic_for_input[0];
                                }
                                $reactions.answer("Укажите значение для аналитики {{$session.current_input_analytic}}");
                                $reactions.buttons([
                                    { text: "Завершить", transition: "/end" }
                                ]);
                            }
                            // ввод закончен
                            else {
                                delete $session.has_graph;
                                $reactions.transition("..");
                            }
                    
                        state: input_be
                            q: $regexp<1300|1600|1800|3800|4000|4200|4400|5200|5400|5500|6700|7000|9900>
                            script:
                                $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                                $session.has_graph = true;
                                $reactions.transition("..");
                                
                        state: input_oe
                            q: $regexp<\d{5,8}>
                            script:
                                $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                                $session.has_graph = true;
                                $reactions.transition("..");
                        
                        state: input_zavod_sklad
                            q: $regexp<[0-9A-z]{4}>
                            script:
                                $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                                $session.has_graph = true;
                                $reactions.transition("..");
                                
                        state: input_mvz_cz
                            q: $regexp<\d{4}[A-z]\d+>
                            script:
                                $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                                $session.has_graph = true;
                                $reactions.transition("..");
                                
                        state: input_buyers_group
                            q: $regexp<[A-z]\d{2}>
                            script:
                                $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                                $session.has_graph = true;
                                $reactions.transition("..");
                                
                        state: input_any_analytic
                            q: $regexp<.*>
                            script:
                                $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                                $session.has_graph = true;
                                $reactions.transition("..");
                
            state: analytics_detailed
                q: $regexp<\W+: [0-9A-zА-я]+.*>
                script:
                    var parsed_be;
                    var result = /БЕ: (\d+).*/i.exec($request.query);
                    if (result.length > 1) { parsed_be = result[1]; }
                    var analytics_lines = $session.analytics_info_detailed[parsed_be];
                    var analytics_txt = "";
                    analytics_lines.forEach(function(line){
                        analytics_txt = analytics_txt + line;
                    });
                    //$session.chosen_be_detailed_info = "БЕ: " + parsed_be + "<br>" + analytics_txt;
                    //$reactions.answer("Информация об аналитиках:<br><br>{{$session.chosen_be_detailed_info}}");
                    $session.chosen_be_detailed_info = $session.graph_txt[parsed_be];
                    $reactions.answer("Информация об аналитиках:<br><br>{{$session.chosen_be_detailed_info}}");
                    $reactions.buttons([
                        { text: "Назад", transition: ".." }, 
                        { text: "Завершить", transition: "/end" }
                    ]);
                    $session.was_in_analytics_detailed = true;
    
    state: show_roles
        script:
            var cells = [];
            var correct_roles = [];
            var roles = $session.final_roles;
            
            if (roles.length > 1) {
                roles.forEach(function(item, i, roles){
                    var cell_text = parseInt(item.AgrNum, 10) + " " + item.AgrTitle;
                    correct_roles.push(cell_text);
                    cells.push(createCell(cell_text, "Роль"));
                });
                pushAnswerWithListCard(cells, [], "Я подобрал для Вас следующие роли");
            }
            else if (roles.length > 0) {
                var cell_text = parseInt(roles[0].AgrNum, 10) + " " + roles[0].AgrTitle;
                correct_roles.push(cell_text);
                cells.push(createCell(cell_text, "Роль"));
                pushAnswerWithListCard(cells, [], "Я подобрал для Вас роль");
            }
            else {
                $reactions.transition("/no_recommended_roles");
            }
            $session.roles_to_choose = correct_roles.join("|");
        
        state: show_role_group_analytics
            # Попадаем в ноду по паттерну: <номер роли><пробел><слова>
            q: $regexp<\d+ \W+>
            # Проверяем, что попали в ноду по роли, которая была дана на выбор пользователю
            script:
                var roles_list = $session.final_roles;
                var is_role_from_list = false;
                var role_info;
                roles_list.forEach(function(item){
                    if (parseInt(item.AgrNum, 10) + " " + item.AgrTitle == $request.query) { 
                        is_role_from_list = true;
                        role_info = item;
                        return;
                    }
                });
                $session.chosen_role = role_info;
                $session.is_role_from_list = is_role_from_list;
            # Если роль не была выбрана по кнопке, выводим список ролей повторно
            if: $session.is_role_from_list == true
                script:
                    // если попали в ноду в первый раз, обращаемся к модели по аналитикам
                    if ($session.role_analytics_mws_response == undefined) {
                        var data = {
                            "role": $session.chosen_role.AgrNum,
                            "be": $session.profile_response.data.d.Bukrs,
                            "oe": $session.profile_response.data.d.Orgeh,
                            "oe2": "10237908",
                            "top": 3
                        }
                        $session.role_analytics_mws_response = post_request_mws("https://dfsa-get-role-analytics-small-1-10000.ci00324559-dev-mws.apps.test-ose.ca.sbrf.ru/dfsa-get-role-analytics-small/predict", {"Data": data});
                        if ($session.role_analytics_mws_response.status != 200) {
                            $reactions.transition("/role_analytics_mws_error");
                        }
                    }
                    var mws_analytics = $session.role_analytics_mws_response.data;
                    
                    if (mws_analytics.length <= 0) {
                        $reactions.transition("./show_role_analytics");
                    }
                    else {
                        // показ наборов аналитик
                        var chosen_role = $session.chosen_role.toParamDef.results;
                        var role_analytics_mapping = {};
                        // Создаем маппинг код аналитики->название аналитики
                        chosen_role.forEach(function(analytic){
                            role_analytics_mapping[analytic.AgrParam.toLowerCase()] = analytic.ParName;
                        });
                        var cells = [];
                        var role_analytics = $session.chosen_role.toParamDef.results;
                        var has_objid = false;
                        var has_kostl = false;
                        role_analytics.forEach(function(analytic) {
                            if (analytic.AgrParam == "KOSTL") {
                                has_kostl = true;
                            }
                            if (analytic.AgrParam == "OBJID") {
                                has_objid = true;
                            }
                        });
                        
                        mws_analytics.forEach(function(analytic_group) {
                            var cell_text = "";
                            Object.keys(analytic_group).forEach(function(analytic) {
                                if (analytic != "role") {
                                    cell_text = cell_text + role_analytics_mapping[analytic.toLowerCase()] + ": " + analytic_group[analytic] + ", ";
                                }
                            });
                            if (has_objid == true) {
                                cell_text = cell_text + role_analytics_mapping["objid"] + ": " + $session.profile_response.data.d.Orgeh + ", ";
                            }
                            if (has_kostl == true) {
                                cell_text = cell_text + role_analytics_mapping["kostl"] + ": " + $session.profile_response.data.d.Kostl + ", ";
                            }
                            cell_text = cell_text.slice(0, -2);
                            cells.push(createCell(cell_text, ""));
                        });
                        cells.push(createCell("Нет подходящего варианта", ""));
                        pushAnswerWithListCard(cells, [], "Выберите подходящую комбинацию аналитик");
                    }
                    
            else:
                go!: /show_roles
                
            state: show_role_analytics
                q: $regexp<([А-я ]+: [А-яA-z0-9- ]+,?)+>
                q: Нет подходящего варианта
                script:
                    var query;
                    // если вернулись из ввода аналитик
                    if ("was_in_analytics_input" in $session) {
                        query = $session.backup_show_role_analytics_query;
                        delete $session.was_in_analytics_input;
                    }
                    else {
                        $session.backup_show_role_analytics_query = $request.query;
                        query = $request.query;
                    }
                
                    var chosen_analytics_group = [];
                    var result = /([А-я ]+: [А-яA-z0-9- ]+[,]?)+/i.exec(query);
                    if (result != undefined) {
                        chosen_analytics_group = result[0].split(", ");
                    }
                    var mws_analytics = {};
                    // если был переход из выбора группы аналитик, парсим группу
                    if (chosen_analytics_group.length > 0) {
                        chosen_analytics_group.forEach(function(analytic) {
                            var analytic_info = analytic.split(": ");
                            mws_analytics[analytic_info[0].toLowerCase()] = analytic_info[1];
                        });
                    }
                    // если был прямой переход, парсим данные из модели
                    else {
                        if ($session.role_analytics_mws_response.data.length > 0) {
                            var group = $session.role_analytics_mws_response.data[0];
                            Object.keys(group).forEach(function(analytic) {
                                if (analytic != "role") {
                                    mws_analytics[analytic.toLowerCase()] = group[analytic];
                                }
                            });
                        }
                    }
                    var analytics_mapping = {
                        #"BUKRS":  $session.profile_response.data.d.Bukrs,
                        #"BLK":    $session.profile_response.data.d.Block,
                        "KOSTL":  $session.profile_response.data.d.Kostl,
                        "OBJID":  $session.profile_response.data.d.Orgeh,
                        #"ZFICTR": $session.profile_response.data.d.Fistl
                    };
                    var chosen_role = $session.chosen_role.toParamDef.results;
                    var role_analytics_mapping = {};
                    // Создаем маппинг код аналитики->название аналитики
                    chosen_role.forEach(function(analytic){
                        role_analytics_mapping[analytic.AgrParam.toLowerCase()] = analytic.ParName.toLowerCase();
                    });
                    var role_analytics = $session.chosen_role.toParamDef.results;
                    var analytics_for_input = [];
                    #var role_analytics_text = "";
                    var cells = [];
                    role_analytics.forEach(function(analytic, i, role_analytics){
                        var analytic_value;
                        var analytic_real_name = role_analytics_mapping[analytic.AgrParam.toLowerCase()];
                        #var analytic_real_name = analytic.AgrParam.toLowerCase();
                        // если значение аналитики пришло из модели
                        if (analytic_real_name in mws_analytics) {
                            analytic_value = mws_analytics[analytic_real_name];
                        }
                        // по дефолту берем значение из профиля пользователя
                        else {
                            analytic_value = analytics_mapping[analytic.AgrParam];
                        }
                        // если значение аналитики было введено пользователем
                        if ($session.analytics_input_values != undefined) {
                            if ((analytic.ParName) in $session.analytics_input_values) {
                                analytic_value = $session.analytics_input_values[analytic.ParName];
                            }
                        };
                        if (analytic_value == undefined) {
                            analytics_for_input.push(analytic);
                        }
                        cells.push(createCell(analytic.ParName + ((analytic_value == undefined) ? "" : ": " + analytic_value), "Изменить"));
                    });
                    $session.analytics_for_input = analytics_for_input;
                    if ($session.analytics_for_input.length > 0) {
                        $reactions.transition("./input_analytics");
                    }
                    else {
                        var header = "Аналитики роли " + parseInt($session.chosen_role.AgrNum, 10) +  " " + $session.chosen_role.AgrTitle;
                        pushAnswerWithListCard(cells, [], header);
                        $reactions.buttons([
                            { text: "Создать заявку", transition: "/create_role_task" },
                            { text: "Завершить", transition: "/end" }
                        ]);
                    }
            
                state: input_analytics
                    q: $regexp<[А-я ]+: [А-яA-z0-9]+>
                    script:
                        $session.was_in_analytics_input = true;
                        var analytic_for_input;
                        var result = /([А-я ]+: [А-яA-z0-9]+)/i.exec($request.query);
                        if (result != undefined) {
                            analytic_for_input = result[0];
                            analytic_for_input = analytic_for_input.split(": ");
                        }
                        if ($session.analytics_input_values == undefined) {
                            $session.analytics_input_values = {};
                        }
                        // если введены не все аналитики
                        if ($session.analytics_for_input.length > 0 || analytic_for_input != undefined) {
                            if ($session.analytics_for_input.length > 0) {
                                var role_analytic = $session.analytics_for_input.shift();
                                $session.current_input_analytic = role_analytic.ParName;
                            }
                            else if (analytic_for_input != undefined) {
                                $session.current_input_analytic = analytic_for_input[0];
                            }
                            $reactions.answer("Укажите {{$session.current_input_analytic}}");
                            $reactions.buttons([
                                { text: "Завершить", transition: "/end" }
                            ]);
                        }
                        // ввод закончен
                        else {
                            $reactions.transition("..");
                        }
                    
                    state: input_be
                        q: $regexp<1300|1600|1800|3800|4000|4200|4400|5200|5400|5500|6700|7000|9900>
                        script:
                            $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                            $reactions.transition("..");
                            
                    state: input_oe
                        q: $regexp<\d{5,8}>
                        script:
                            $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                            $reactions.transition("..");
                    
                    state: input_zavod_sklad
                        q: $regexp<[0-9A-z]{4}>
                        script:
                            $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                            $reactions.transition("..");
                            
                    state: input_mvz_cz
                        q: $regexp<\d{4}[A-z]\d+>
                        script:
                            $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                            $reactions.transition("..");
                            
                    state: input_buyers_group
                        q: $regexp<[A-z]\d{2}>
                        script:
                            $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                            $reactions.transition("..");
                            
                    state: input_any_analytic
                        q: $regexp<.*>
                        script:
                            $session.analytics_input_values[$session.current_input_analytic] = $request.query;
                            $reactions.transition("..");
    
    state: create_role_task
        a: Заявка <a href="#">SD0143651240</a> создана.
        go!: /end
    
    state: role_analytics_mws_error
        a: При выполнении запроса к модели по аналитикам возникла ошибка. {{JSON.stringify($session.role_analytics_mws_response)}}
        go!: /end
    
    state: role_list_error
        a: При выполнении запроса к модели по ролям возникла ошибка. {{JSON.stringify($session.role_list_response)}}
        go!: /end
        
    state: get_roles_from_sap_error
        a: При выполнении запроса к списку ролей пользователя возникла ошибка. {{JSON.stringify($session.get_roles_from_sap_response)}}
        go!: /end
    
    state: no_recommended_roles
        a: Рекомендуемые роли не определены
        go!: /end
    
    state: end
        a: Спасибо за обращение!
        EndSession: