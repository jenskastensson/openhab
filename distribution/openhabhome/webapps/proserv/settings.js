function getEmail() {
  $.ajax({
    url : '/CMD?ProservEmail=?',
    success : function (response) {
      $.ajax({
        url : '/rest/items/ProservEmail/state',
        timeout : 5000,
        success : function (response) {
          current_email = response;
        },
        failure : function (response) {
          alert('Error connecting to server!');
        }
      });
    },
    failure : function (response) {
      title = OpenHAB.i18n_strings[ui_language].timeout;
      msg = OpenHAB.i18n_strings[ui_language].the_operation_timed_out_waiting;
      //Ext.Msg.alert(title, msg);
    }
  });
}
function getIP() {
  $.ajax({
    url : '/CMD?ProservIP=?',
    success : function (response) {
      $.ajax({
        url : '/rest/items/ProservIP/state',
        timeout : 5000,
        success : function (response) {
          current_ip = response;
        },
        failure : function (response) {
          alert('Error connecting to server!');
        }
      });
    },
    failure : function (response) {
      title = OpenHAB.i18n_strings[ui_language].timeout;
      msg = OpenHAB.i18n_strings[ui_language].the_operation_timed_out_waiting;
      //Ext.Msg.alert(title, msg);
    }
  });
}
var current_ip = "";
var current_email = "";
getEmail();
getIP();

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
  width : 400,
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

//
// Save history data
//
$('#save_history_data_btn').click(function (event) {
  $('#save_history_data_dlg').dialog('open');
  event.preventDefault();
});
$('#save_history_data_btn').button({
  label : OpenHAB.i18n_strings[ui_language].button_save_history_data
});
$('#save_history_data_dlg').dialog({
  title : OpenHAB.i18n_strings[ui_language].button_save_history_data,
  open : function () {
    $(this).html(OpenHAB.i18n_strings[ui_language].this_will_save_a_snapshot);
  },
  autoOpen : false,
  width : 500,
  modal : true,
  buttons : [{
      text : OpenHAB.i18n_strings[ui_language].button_save_history_data,
      click : function () {
        var stepOneOk = false;
        $.when(
          $.ajax({
            timeout : 600000,
            url : '/CMD?ProservExportCsvFiles=START',
            success : function () {
              $.ajax({
                async: false,
                url : '/rest/items/ProservExportCsvFiles/state',
                timeout : 5000,
                success : function (response) {
                  if (response == 'SUCCESS') {
                    stepOneOk = true;
                    // not success yet, do the backup as well
                    //$('#save_history_data_btn').button('option', 'icons', {
                    //  primary : 'ui-icon-circle-check'
                    //});
                  } else if (response.indexOf('FAILED') >= 0) {
                    $('#save_history_data_btn').button('option', 'icons', {
                      primary : 'ui-icon-alert'
                    });
                    title = OpenHAB.i18n_strings[ui_language].the_operation_failed;
                    msg = OpenHAB.i18n_strings[ui_language].error_message_from_server;
                    $.alert(msg + response.replace('FAILED:', ''), title);
                  }
                },
                error : function () {
                  $('#save_history_data_btn').button('option', 'icons', {
                    primary : 'ui-icon-alert'
                  });
                }
              });
            },
            error : function () {
              $('#save_history_data_btn').button('option', 'icons', {
                primary : 'ui-icon-alert'
              });
              title = OpenHAB.i18n_strings[ui_language].the_operation_failed;
              msg = OpenHAB.i18n_strings[ui_language].the_operation_timed_out_waiting;
              $.alert(msg, title);
            }
          })).done(function () {

          if (stepOneOk == true) {
            $.ajax({
              timeout : 10000,
              url : '/CMD?ProservBackupRrd=START',
              success : function () {
                $.ajax({
                  url : '/rest/items/ProservBackupRrd/state',
                  timeout : 5000,
                  success : function (response) {
                    if (response == 'SUCCESS') {
                      $('#save_history_data_btn').button('option', 'icons', {
                        primary : 'ui-icon-circle-check'
                      });
                    } else if (response.indexOf('FAILED') >= 0) {
                      $('#save_history_data_btn').button('option', 'icons', {
                        primary : 'ui-icon-alert'
                      });
                      title = OpenHAB.i18n_strings[ui_language].the_operation_failed;
                      msg = OpenHAB.i18n_strings[ui_language].error_message_from_server;
                      $.alert(msg + response.replace('FAILED:', ''), title);
                    }
                  },
                  error : function () {
                    $('#save_history_data_btn').button('option', 'icons', {
                      primary : 'ui-icon-alert'
                    });
                  }
                });
              },
              error : function () {
                $('#save_history_data_btn').button('option', 'icons', {
                  primary : 'ui-icon-alert'
                });
                title = OpenHAB.i18n_strings[ui_language].the_operation_failed;
                msg = OpenHAB.i18n_strings[ui_language].the_operation_timed_out_waiting;
                $.alert(msg, title);
              }
            });
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

//
// Send history data
//
$('#send_history_data_btn').click(function (event) {
  $('#send_history_data_dlg').dialog('open');
  event.preventDefault();
});
$('#send_history_data_btn').button({
  label : OpenHAB.i18n_strings[ui_language].button_send_history_data
});
$('#send_history_data_dlg').dialog({
  title : OpenHAB.i18n_strings[ui_language].confirm_sending_history_data,
  open : function () {
    $(this).html(OpenHAB.i18n_strings[ui_language].assuming_you_have_exported_history);
  },
  autoOpen : false,
  width : 500,
  modal : true,
  buttons : [{
      text : OpenHAB.i18n_strings[ui_language].button_send_history_data,
      click : function () {
        $.ajax({
          timeout : 240000,
          url : '/CMD?ProservSendCsvFiles=START',
          success : function () {
            $.ajax({
              url : '/rest/items/ProservSendCsvFiles/state',
              timeout : 5000,
              success : function (response) {
                if (response == 'SUCCESS') {
                  $('#send_history_data_btn').button('option', 'icons', {
                    primary : 'ui-icon-circle-check'
                  });
                } else if (response.indexOf('FAILED') >= 0) {
                  $('#send_history_data_btn').button('option', 'icons', {
                    primary : 'ui-icon-alert'
                  });
                  title = OpenHAB.i18n_strings[ui_language].the_operation_failed;
                  msg = OpenHAB.i18n_strings[ui_language].error_message_from_server;
                  $.alert(msg + response.replace('FAILED:', ''), title);
                }
              },
              error : function () {
                $('#send_history_data_btn').button('option', 'icons', {
                  primary : 'ui-icon-alert'
                });
              }
            });
          },
          error : function () {
            $('#send_history_data_btn').button('option', 'icons', {
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

//
// Email configuration
//
$('#set_email_btn').click(function (event) {
  $('#email_input').button().addClass('ui-textfield');
  $('#email_text').html(OpenHAB.i18n_strings[ui_language].confirm_email_msg);
  $('#email_input').val(current_email);
  $('#set_email_dlg').dialog('open');
  event.preventDefault();
});

$('#set_email_btn').button({
  label : OpenHAB.i18n_strings[ui_language].button_configure_email
});
$('#set_email_dlg').dialog({
  title : OpenHAB.i18n_strings[ui_language].button_configure_email,
  autoOpen : false,
  width : 500,
  modal : true,
  buttons : [{
      text : OpenHAB.i18n_strings[ui_language].save,
      click : function () {
        new_email = $('#email_input').val();
        if (new_email != current_email) {

          $.ajax({
            url : '/CMD?ProservEmail=' + new_email,
            timeout : 5000,
            success : function (response) {
              $.ajax({
                url : '/rest/items/ProservEmail/state',
                timeout : 5000,
                success : function (response) {
                  if (response != new_email) {
                    title = OpenHAB.i18n_strings[ui_language].the_operation_failed;
                    msg = OpenHAB.i18n_strings[ui_language].changing_email_failed;
                    $.alert(msg, title);
                  } else {
                    $('#set_email_btn').button('option', 'icons', {
                      primary : 'ui-icon-circle-check'
                    });
                    current_email = new_email;
                  }
                }
              });
            },
            error : function () {
              $('#set_email_btn').button('option', 'icons', {
                primary : 'ui-icon-alert'
              });
              title = OpenHAB.i18n_strings[ui_language].timeout;
              msg = OpenHAB.i18n_strings[ui_language].the_operation_timed_out_waiting;
              $.alert(msg, title);
            }
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
// IP configuration
//
$('#set_ip_btn').click(function (event) {
  $('#ip_input').button().addClass('ui-textfield');
  $('#ip_text').html(OpenHAB.i18n_strings[ui_language].confirm_proserv_ip_msg);
  $('#ip_input').val(current_ip);
  $('#set_ip_dlg').dialog('open');
  event.preventDefault();
});

$('#set_ip_btn').button({
  label : OpenHAB.i18n_strings[ui_language].button_configure_ip
});
$('#set_ip_dlg').dialog({
  title : OpenHAB.i18n_strings[ui_language].button_configure_ip,
  autoOpen : false,
  width : 500,
  modal : true,
  buttons : [{
      text : OpenHAB.i18n_strings[ui_language].save,
      click : function () {
        new_ip = $('#ip_input').val();
        if (new_ip != current_ip) {
          $.ajax({
            url : '/CMD?ProservIP=' + new_ip,
            timeout : 5000,
            success : function (response) {
              $.ajax({
                url : '/rest/items/ProservIP/state',
                timeout : 5000,
                success : function (response) {
                  if (response != new_ip) {
                    title = OpenHAB.i18n_strings[ui_language].the_operation_failed;
                    msg = OpenHAB.i18n_strings[ui_language].changing_ip_failed;
                    $.alert(msg, title);
                  } else {
                    $('#set_ip_btn').button('option', 'icons', {
                      primary : 'ui-icon-circle-check'
                    });
                    current_ip = new_ip;
                  }
                }
              });
            },
            error : function () {
              $('#set_ip_btn').button('option', 'icons', {
                primary : 'ui-icon-alert'
              });
              title = OpenHAB.i18n_strings[ui_language].timeout;
              msg = OpenHAB.i18n_strings[ui_language].the_operation_timed_out_waiting;
              $.alert(msg, title);
            }
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