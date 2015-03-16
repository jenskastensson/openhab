      

      var language_array = new Array();
      for (var i in OpenHAB.i18n_strings) {
        language_array.push({
          text : OpenHAB.i18n_strings[i].language_name,
          value : i
        })
      }

      //
      // Language selection
      //
      $('#select_language_btn').click(function (event) {
        $('#language_selectmenu').selectmenu({
          width : 'auto',
        });
        var sel = $('#language_selectmenu').appendTo('#select_language_dlg');
        sel.empty();
        $(language_array).each(function () {
          sel.append($('<option>').attr('value', this.value).text(this.text));
        });
        $('#language_selectmenu').val(ui_language);
        $("#language_selectmenu").selectmenu("refresh");

        $('#select_language_dlg').dialog('open');
        event.preventDefault();
      });

      $('#select_language_btn').button({
        label : OpenHAB.i18n_strings[ui_language].select_language
      });
      $('#select_language_dlg').dialog({
        title : OpenHAB.i18n_strings[ui_language].select_language,
        autoOpen : false,
        width : 500,
        height : 200,
        modal : true,
        buttons : [{
            text : OpenHAB.i18n_strings[ui_language].change_language,
            click : function () {
              new_ui_language = $('#language_selectmenu').val();
              if (new_ui_language != ui_language) {
                title = OpenHAB.i18n_strings[ui_language].restart_server;
                msg = OpenHAB.i18n_strings[ui_language].this_will_restart_the_software;
                $.alertsync(msg, title, function () {
                  $.ajax({
                    url : '/CMD?ProservLanguage=' + new_ui_language,
                    timeout : 5000,
                    success : function (response) {
                      $.ajax({
                        url : '/rest/items/ProservLanguage/state',
                        timeout : 5000,
                        success : function (response) {
                          if (response != new_ui_language) {
                            title = OpenHAB.i18n_strings[ui_language].the_operation_failed;
                            msg = OpenHAB.i18n_strings[ui_language].changing_language_failed;
                            $.alert(msg, title);
                          } else {
                            title = OpenHAB.i18n_strings[ui_language].restart_server;
                            msg = OpenHAB.i18n_strings[ui_language].need_to_restart_for_changes_to_take_effect;
                            $.alertnok(msg, title);
                            setTimeout(function () {
                              location.assign('./settings.html');
                            }, 120000);
                          }
                        }
                      });
                    },
                    error : function () {
                      $('#select_language_btn').button('option', 'icons', {
                        primary : 'ui-icon-alert'
                      });
                      title = OpenHAB.i18n_strings[ui_language].timeout;
                      msg = OpenHAB.i18n_strings[ui_language].the_operation_timed_out_waiting;
                      $.alert(msg, title);
                    }
                  });
                });
                $(this).dialog('close');

              }
              $(this).dialog('close');

            }
          }, {
            text : OpenHAB.i18n_strings[ui_language].cancel,
            click : function () {
              $(this).dialog('close');
            }
          }
        ]
      });

      //
      // Reset history data
      //
      $('#reset_history_data_btn').click(function (event) {
        $('#reset_history_data_dlg').dialog('open');
        event.preventDefault();
      });
      $('#reset_history_data_btn').button({
        label : OpenHAB.i18n_strings[ui_language].button_save_reset_history_data
      });
      $('#reset_history_data_dlg').dialog({
        title : OpenHAB.i18n_strings[ui_language].confirm_reset_of_history_data,
        open : function () {
          $(this).html(OpenHAB.i18n_strings[ui_language].are_you_sure_you_want_to_reset);
        },
        autoOpen : false,
        width : 500,
        modal : true,
        buttons : [{
            text : OpenHAB.i18n_strings[ui_language].button_save_reset_history_data,
            click : function () {
              $.ajax({
                timeout : 5000,
                url : '/CMD?ProservBackupResetRrd=START',
                success : function () {
                  $.ajax({
                    url : '/rest/items/ProservBackupResetRrd/state',
                    timeout : 5000,
                    success : function (response) {
                      if (response == 'SUCCESS') {
                        $('#reset_history_data_btn').button('option', 'icons', {
                          primary : 'ui-icon-circle-check'
                        });
                      } else if (response.indexOf('FAILED') >= 0) {
                        $('#reset_history_data_btn').button('option', 'icons', {
                          primary : 'ui-icon-alert'
                        });
                        title = OpenHAB.i18n_strings[ui_language].the_operation_failed;
                        msg = OpenHAB.i18n_strings[ui_language].error_message_from_server;
                        $.alert(msg + response.replace('FAILED:', ''), title);
                      }
                    },
                    error : function () {
                      $('#reset_history_data_btn').button('option', 'icons', {
                        primary : 'ui-icon-alert'
                      });
                    }
                  });
                },
                error : function () {
                  $('#reset_history_data_btn').button('option', 'icons', {
                    primary : 'ui-icon-alert'
                  });
                  title = OpenHAB.i18n_strings[ui_language].the_operation_failed;
                  msg = OpenHAB.i18n_strings[ui_language].the_operation_timed_out_waiting;
                  $.alert(msg, title);
                }
              });
              $(this).dialog('close');
            }
          }, {
            text : OpenHAB.i18n_strings[ui_language].cancel,
            click : function () {
              $(this).dialog('close');
            }
          }
        ]
      });