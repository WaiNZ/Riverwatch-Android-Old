// Generated by CoffeeScript 1.3.3
(function() {
  var initialize;

  initialize = function() {
    var elem, map, mapCenter, mapOptions;
    mapCenter = new google.maps.LatLng(-34.397, 150.644);
    mapOptions = {
      zoom: 8,
      center: mapCenter,
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    elem = document.getElementById('map_canvas');
    return map = new google.maps.Map(elem, mapOptions);
  };

  google.maps.event.addDomListener(window, 'load', initialize);

}).call(this);
