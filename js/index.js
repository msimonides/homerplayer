(function() {

  var VIDEO_URL = 'https://www.youtube.com/embed/RfLkoLtxzng?vq=large&autoplay=1&autohide=1&border=0&egm=0&showinfo=0';
  var WIDTH = 853;
  var HEIGHT = 480;
  var BUTTONS_OFFSET = 10;

  function beforeOverlayShow() {
    var buttons = $('.buttons ul');
    var offset = buttons.offset();
    var height = buttons.height();
    buttons.parent().css( { height: height } );
  
    var dst_top = ($(window).height() + HEIGHT) / 2 + BUTTONS_OFFSET;
    buttons.addClass('overlay');
    buttons.css( { top: offset.top, left: '0px' } );
    buttons.animate({ top: dst_top  }, 700);
  }
  
  function onOverlayHide() {
    var buttons = $('.buttons ul');
    buttons.removeClass('overlay');
    buttons.parent().css( { height: '' } );
  }
  
  
  $(document).ready(function() {
    $('.play-button a').click(function(event) {
      event.preventDefault();
  
      gaVirtualPageView('/index-video', 'Main page vide');
  
      var lightview_options = {
        width: WIDTH,
        height: HEIGHT,
        initialDimensions: { width: WIDTH, height: HEIGHT }
      }

      lightview_options.onHide = onOverlayHide;
      beforeOverlayShow();

      Lightview.show({
        url: VIDEO_URL,
        type: 'iframe',
        options: lightview_options
      });

    });
  });

})()
