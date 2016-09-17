(
	define (
		script-fu-megamek-unit-shading-v3
		image
		original-layer
	)
	
	(
		let* (
			(old-bg (car (gimp-palette-get-background)))
			(layer-source-layer original-layer)
			(layer-working 0)
			(layer-to-process 0)
			(layergroup-unit-shading 0)
			(layer-white-base 0)
			(layer-shadow-1-sw-side 0)
			(layer-highlight-NE-side 0)
			(layer-enhanced-panel-detail 0)
			(layergroup-finished-unit 0)
			(layer-color-details 0)
			(layer-finished-unit-body 0)
			(layer-unit-shadow 0)
			(shadow-x 0)
			(shadow-y 0)
			(shadow-blur 0)
			(shadow-color '(255 255 255))
			(shadow-opacity 100)
			(shadow-resize FALSE)
			(brightness 0)
			(contrast 0)
		)


		(gimp-undo-push-group-start image)
		
		;this was a bad idea.  will cause the script to error if the image is already in rbg.  Better to handle this with bimp.
		;(gimp-image-convert-rgb image)
		
		;;;;;;;;;; Create Shading Layer Group ;;;;;;;;;;;;
		(set! layer-working (car (gimp-layer-group-new image)))
		(gimp-image-insert-layer image layer-working 0 -1)
		(gimp-layer-set-name layer-working "Unit Shading")
		(set! layergroup-unit-shading layer-working)
		;;;;;;;;;; Create Shading Layer Group ;;;;;;;;;;;;
		
		
		;;;;;;;;;;;; Create Processing Layer ;;;;;;;;;;;;;
		(set! layer-working (car (gimp-layer-new-from-drawable layer-source-layer image)))
		(gimp-image-insert-layer image layer-working layergroup-unit-shading -1)
		(gimp-layer-set-name layer-working "To Process")
		(set! layer-to-process layer-working)

		(gimp-item-set-visible layer-source-layer FALSE)
		(gimp-layer-set-name layer-source-layer "Image Source")
		;;;;;;;;;;;; Create Processing Layer ;;;;;;;;;;;;;


		;;;;;;;;;;;;;;;;;; White Base ;;;;;;;;;;;;;;;;;;;;
		(set! shadow-x 0)
		(set! shadow-y 0)
		(set! shadow-blur 0)
		(set! shadow-color '(255 255 255))
		(set! shadow-opacity 100)

		(script-fu-drop-shadow image layer-to-process shadow-x shadow-y shadow-blur shadow-color shadow-opacity shadow-resize)
		
		(set! layer-working (car (gimp-image-get-layer-by-name image "Drop Shadow")))
		(set! layer-white-base layer-working)
		(gimp-layer-set-name layer-working "White Base")
		(gimp-image-set-active-layer image layer-to-process)
		;;;;;;;;;;;;;;;;;; White Base ;;;;;;;;;;;;;;;;;;;;

       
		;;;;;;;;;;;;;;;;;; Shadow 1 - SW Side ;;;;;;;;;;;;;;;;;;;;
		(set! shadow-x -3)
		(set! shadow-y 3)
		(set! shadow-blur 4)
		(set! shadow-color '(0 0 0))
		(set! shadow-opacity 55)

		(script-fu-drop-shadow image layer-to-process shadow-x shadow-y shadow-blur shadow-color shadow-opacity shadow-resize)

		(set! layer-working (car (gimp-image-get-layer-by-name image "Drop Shadow")))
		(set! layer-shadow-1-sw-side layer-working)
		(gimp-layer-set-name layer-working "Shadow 1 - SW Side")
		(gimp-layer-set-mode layer-working DARKEN-ONLY-MODE)
		(gimp-image-set-active-layer image layer-to-process)
		;;;;;;;;;;;;;;;;;; Shadow 1 - SW Side ;;;;;;;;;;;;;;;;;;;;

       
		;;;;;;;;;;;;;;;;; Highlight - NE Side ;;;;;;;;;;;;;;;;;;;;
		(set! shadow-x 3)
		(set! shadow-y -3)
		(set! shadow-blur 4)
		(set! shadow-color '(255 255 255))
		(set! shadow-opacity 100)

		(script-fu-drop-shadow image layer-to-process shadow-x shadow-y shadow-blur shadow-color shadow-opacity shadow-resize)

		(set! layer-working (car (gimp-image-get-layer-by-name image "Drop Shadow")))
		(set! layer-highlight-NE-side layer-working)
		(gimp-layer-set-name layer-working "Highlight - NE Side")
		(gimp-layer-set-mode layer-working LIGHTEN-ONLY-MODE)
		(gimp-image-set-active-layer image layer-to-process)
		;;;;;;;;;;;;;;;;; Highlight - NE Side ;;;;;;;;;;;;;;;;;;;;
		
		
		;;;;;;;;;;;;;;;; Enhanced Panel Detail ;;;;;;;;;;;;;;;;;;;
		(set! brightness 0)
		(set! contrast 85)
		
		;(gimp-item-set-visible layer-to-process FALSE)
		(set! layer-working (car (gimp-layer-new-from-visible image image "Enhanced Panel Detail")))
		(set! layer-enhanced-panel-detail layer-working)
		
		(gimp-image-insert-layer image layer-working 0 -1)
		;(gimp-image-lower-layer image layer-working)
		(gimp-brightness-contrast layer-working brightness contrast)
		;(gimp-item-set-visible layer-to-process TRUE)
		;;;;;;;;;;;;;;;; Enhanced Panel Detail ;;;;;;;;;;;;;;;;;;;

		
		;;;;;;;;;;;;;;;;;;;;; White To Alpha ;;;;;;;;;;;;;;;;;;;;;
 		(plug-in-colortoalpha 1 image layer-enhanced-panel-detail '(255 255 255))
 		(plug-in-colortoalpha 1 image layer-to-process '(255 255 255))
		(gimp-image-set-active-layer image layer-to-process)
 		;;;;;;;;;;;;;;;;;;;;; White To Alpha ;;;;;;;;;;;;;;;;;;;;;
      
       
  		;;;;;;;;;;;;;;;;;;;;;;; Shading ;;;;;;;;;;;;;;;;;;;;;;;;;;
		(set! shadow-x 0)
		(set! shadow-y 0)
		(set! shadow-blur 3)
		(set! shadow-color '(0 0 0))
		(set! shadow-opacity 43)

		(script-fu-drop-shadow image layer-to-process shadow-x shadow-y shadow-blur shadow-color shadow-opacity shadow-resize)

		(set! layer-working (car (gimp-image-get-layer-by-name image "Drop Shadow")))
		(gimp-layer-set-name layer-working "Shading")
		(gimp-layer-set-mode layer-working DARKEN-ONLY-MODE)
		(gimp-image-set-active-layer image layer-to-process)
  		;;;;;;;;;;;;;;;;;;;;;;; Shading ;;;;;;;;;;;;;;;;;;;;;;;;;;
     
       
  		;;;;;;;;;;;;;;;;;;;; Detail Shading ;;;;;;;;;;;;;;;;;;;;;;
		(set! shadow-x 0)
		(set! shadow-y 0)
		(set! shadow-blur 1)
		(set! shadow-color '(0 0 0))
		(set! shadow-opacity 79)

		(script-fu-drop-shadow image layer-to-process shadow-x shadow-y shadow-blur shadow-color shadow-opacity shadow-resize)

		(set! layer-working (car (gimp-image-get-layer-by-name image "Drop Shadow")))
		(gimp-layer-set-name layer-working "Detail Shading")
		(gimp-layer-set-mode layer-working DARKEN-ONLY-MODE)
		(gimp-image-set-active-layer image layer-to-process)
  		;;;;;;;;;;;;;;;;;;;; Detail Shading ;;;;;;;;;;;;;;;;;;;;;;

       
		;;;;;;;;; Make new layer from composite shading ;;;;;;;;;;
		(set! brightness 33)
		(set! contrast 20)
		
		(gimp-item-set-visible layer-to-process FALSE)
		(gimp-item-set-visible layer-enhanced-panel-detail FALSE)
		
		(set! layer-working (car (gimp-layer-new-from-visible image image "Composite Shading")))
		(gimp-image-insert-layer image layer-working 0 -1)
		(gimp-image-lower-layer image layer-working)
		(gimp-brightness-contrast layer-working brightness contrast)
		
		;(gimp-item-set-visible layer-to-process TRUE)
		(gimp-item-set-visible layer-enhanced-panel-detail TRUE)
		;;;;;;;;; Make new layer from composite shading ;;;;;;;;;;


		;;; Rebalance visibility of Enhanced Panel Detail Layer ;;
		;(set! shadow-opacity 55.0)
		(set! shadow-opacity 35.0)
       		(gimp-layer-set-opacity layer-enhanced-panel-detail shadow-opacity)
		;;; Rebalance visibility of Enhanced Panel Detail Layer ;;
		
		
		;;;;;;;;;;;;;; Dump The Old Processing Layer ;;;;;;;;;;;;;
		(gimp-image-remove-layer image layer-to-process)
		;;;;;;;;;;;;;; Dump The Old Processing Layer ;;;;;;;;;;;;;
		
		
		;;;;;;;;;;; Create Finished Layer Group ;;;;;;;;;;;
		(gimp-image-set-active-layer image layer-source-layer)

		(set! layer-working (car (gimp-layer-group-new image)))
		(gimp-image-insert-layer image layer-working 0 -1)
		(gimp-layer-set-name layer-working "Finished Unit")
		(gimp-image-raise-item-to-top image layer-working)
		(set! layergroup-finished-unit layer-working)
		;;;;;;;;;;; Create Finished Layer Group ;;;;;;;;;;;
		
		
		;;;;;;;;;;;;; Make Finished Unit Body Layer ;;;;;;;;;;;;;;
		(set! layer-working (car (gimp-layer-new-from-visible image image "Finished Unit Body")))
		(gimp-image-insert-layer image layer-working 0 -1)
		(gimp-desaturate-full layer-working DESATURATE-LIGHTNESS)
		(set! layer-finished-unit-body layer-working)
		
		(gimp-item-set-visible layergroup-unit-shading FALSE)
		;;;;;;;;;;;;; Make Finished Unit Body Layer ;;;;;;;;;;;;;;

		
  		;;;;;;;;;;;;;;;; Make Unit Shadow Layer ;;;;;;;;;;;;;;;;;;
		(set! shadow-x 0)
		(set! shadow-y 0)
		(set! shadow-blur 10)
		(set! shadow-color '(0 0 2))
		(set! shadow-opacity 82)

		(script-fu-drop-shadow image layer-finished-unit-body shadow-x shadow-y shadow-blur shadow-color shadow-opacity shadow-resize)
		(set! layer-working (car (gimp-image-get-layer-by-name image "Drop Shadow")))
		(gimp-layer-set-name layer-working "Unit Shadow")
		
		(gimp-colorize layer-working 214.0 52.0 2.0)
		
		(set! layer-unit-shadow layer-working)
		
  		;;;;;;;;;;;;;;;; Make Unit Shadow Layer ;;;;;;;;;;;;;;;;;;
  		
  		
  		;;;;;;;;;;;;;;; Make Color Details Layer ;;;;;;;;;;;;;;;;;
		(gimp-image-set-active-layer image layer-finished-unit-body)
		
		(
			set! layer-working (
				car (
					gimp-layer-new
					image
					(car (gimp-image-width image))
					(car (gimp-image-height image))
					RGBA-IMAGE
					"Color Details Go Here"
					100.0
					NORMAL-MODE
				)
			)
		)

		(gimp-image-insert-layer image layer-working 0 -1)
		
		(gimp-layer-set-name layer-working "Color Details Go Here")
		(set! layer-color-details layer-working)
  		;;;;;;;;;;;;;;; Make Color Details Layer ;;;;;;;;;;;;;;;;;
       

		;;;;;;;;;;;;;;;;;;;;;; Cleanup ;;;;;;;;;;;;;;;;;;;;;;;;;;;
		(gimp-palette-set-background old-bg)
		(gimp-image-set-active-layer image layer-color-details)
		
		(gimp-undo-push-group-end image)
		(gimp-displays-flush)
		;;;;;;;;;;;;;;;;;;;;;; Cleanup ;;;;;;;;;;;;;;;;;;;;;;;;;;;
	)
)
       
       
       

(
	script-fu-register "script-fu-megamek-unit-shading-v3"
		_"_Megamek Unit Shading V3..."
		_"Turns boring flat MegaMek units into shaded 3d units.  By Colonel Sanders Lite"
		"Colonel Sanders Lite"
		"I Don't Care"
		"2016/8/21"
		"RGBA"
		SF-IMAGE      "Image"           0
		SF-DRAWABLE   "Drawable"        0
)

(script-fu-menu-register "script-fu-megamek-unit-shading-v3"
	 "<Image>/Filters/Light and Shadow/Shadow")
