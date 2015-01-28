var langCode='en';
var langcodes=['en','fr','de'];var lang=langCode.toLowerCase();lang=lang.substr(0,2);var dest=window.location.origin+'/proserv/index.html';for(i=langcodes.length-1;i>=0;i--){if(lang==langcodes[i]){dest=dest.substr(0,dest.lastIndexOf('.'))+'-'+lang.substr(0,2)+dest.substr(dest.lastIndexOf('.'));window.location.replace?window.location.replace(dest):window.location=dest;}}
