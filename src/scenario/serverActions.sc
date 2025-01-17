# Когда приходит ServerAction, сообщение прилетает в состояние, 
# с условием на вход action_id.
# пример:
#     event!: action_id

theme: /
    
    state: ЗаданиеВыполнено
        event!: done
        event!: DONE

        script:
            $temp.gender = $request.rawRequest.payload.character.gender;
            
        if: $request && $request.data && $request.data.eventData && $request.data.eventData.note
            if: $temp.gender == "male"
                a: Закрыл {{ $request.data.eventData.note }}! Молодец!
            elseif: $temp.gender == "female"
                a: Закрыла {{ $request.data.eventData.note }}! Молодец!
            else:
                a: Закрыто
        else:
            random: 
                a: Молодец!
                a: Красавчик!
                a: Супер!
                
        buttons:
            "Запиши купить молоко"
            "Добавь запись помыть машину"
            "Выйди"

           
    state: ДобавленаНоваяЗапись
        event!: note_added
        event!: NOTE_ADDED

        random: 
            a: Ок!
            a: Ага!
            a: Сделано!
        
        buttons:
            "Запиши купить молоко"
            "Добавь запись помыть машину"
            "Выйди"
            