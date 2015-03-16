     $.extend({
       alert : function (message, title) {
         $("<div></div>").dialog({
           // Remove the closing 'X' from the dialog
           open : function (event, ui) {
             $(".ui-dialog-titlebar-close").hide();
           },
           buttons : {
             "Ok" : function () {
               $(this).dialog("close");
             }
           },
           width: 400,
           resizable : true,
           title : title,
           modal : true
         }).text(message);
       }
     });
     $.extend({
       alertnok : function (message, title) {
         $("<div></div>").dialog({
           // Remove the closing 'X' from the dialog
           open : function (event, ui) {
             $(".ui-dialog-titlebar-close").hide();
           },
           width: 400,
           resizable : true,
           title : title,
           modal : true
         }).text(message);
       }
     });
     
     $.extend({
       alertsync : function (message, title, okAction) {
         $("<div></div>").dialog({
           // Remove the closing 'X' from the dialog
           open : function (event, ui) {
             $(".ui-dialog-titlebar-close").hide();
           },
           buttons : {
             "Ok" : function () {
               $(this).dialog("close");
               okAction();
             }
           },
           close : function (event, ui) {
             $(this).remove();
           },
           width: 400,           
           resizable : true,
           title : title,
           modal : true
         }).text(message);
       }
     });     
     $.extend({
       confirm : function (message, title, okAction) {
         $("<div></div>").dialog({
           // Remove the closing 'X' from the dialog
           open : function (event, ui) {
             $(".ui-dialog-titlebar-close").hide();
           },
           buttons : {
             "Ok" : function () {
               $(this).dialog("close");
               okAction();
             },
             "Cancel" : function () {
               $(this).dialog("close");
             }
           },
           close : function (event, ui) {
             $(this).remove();
           },
           resizable : true,
           title : title,
           modal : true
         }).text(message);
       }
     });