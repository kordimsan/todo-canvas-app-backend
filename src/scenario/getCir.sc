theme: /

    state: getCir
        q!: (~показать|покажи)
            $AnyText::anyText
        
        script:
            getTable({"color": [
                    ["#FA6D20", "#FFFFFF", "#FFFFFF", "#FFFFFF"],
                    ["#24B23E", "#FFFFFF", "#FFFFFF", "#FFFFFF"],
                    ["#FA6D20", "#FF4D5E", "#24B23E", "#FFFFFF"],
                    ["#FA6D20", "#FF4D5E", "#FFFFFF", "#24B23E"],
            ]}, $context);
            
        a: Вот!