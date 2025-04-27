.. _RenderShapeFromSumFile:

######################
RenderShapeFromSumFile
######################

*****
Usage
*****

RenderShapeFromSumFile generates a simulated image from a sumfile.

.. include:: ../toolDescriptions/RenderShapeFromSumFile.txt
    :literal:
  
********
Examples
********

:download:`This<./support_files/g_12570mm_alt_obj_0000n00000_v014.obj>` 
is a very low resolution shape model of Bennu from the OSIRIS-REx mission.  You can substitute higher resolution
models from ???.  You will also need :download:`M605862153F5.SUM<support_files/M605862153F5.SUM>` which contains  
observation geometry we will use to create a simulated image.

Run RenderShapeFromSumFile:

::

    RenderShapeFromSumFile -model g_12570mm_alt_obj_0000n00000_v014.obj  -sumFile M605862153F5.SUM -output M605862153F5.png

.. container:: figures-row

    .. figure:: images/M605862153F5_12570.png
        :alt: This simulated image uses the supplied 12 m/pixel shape model.

        This simulated image uses the supplied 12 m/pixel shape model.

    .. figure:: images/M605862153F5_00870.png
        :alt: This simulated image uses a 87 cm/pixel shape model.

        This simulated image uses a 87 cm/pixel shape model.

    .. figure:: images/ocams20190314t190123s972_map_iofl2pan_77236.png
        :alt: This is the actual image from MAPCAM.

        This is the actual image from MAPCAM.

Generate a FITS file.  The FITS header contains additional information such as illumination
angles and range for each pixel in the image:

::

    RenderShapeFromSumFile -model g_12570mm_alt_obj_0000n00000_v014.obj  -sumFile M605862153F5.SUM -output M605862153F5.fits

Here is the header from the FITS file:

::

    SIMPLE  =                    T / Java FITS: Wed Jan 15 14:53:52 EST 2025        
    BITPIX  =                  -64 / bits per data element                          
    NAXIS   =                    3 / dimensionality of data                         
    NAXIS1  =                 2048 / n'th data dimension                            
    NAXIS2  =                 2048 / n'th data dimension                            
    NAXIS3  =                   11 / n'th data dimension                            
    EXTEND  =                    T / allow extensions                               
    UTC     = '2019 MAR 14 19:01:23.961' / Time from the SUM file                   
    TITLE   = 'M605862153F5'       / Title of SUM file                              
    PLANE1  = 'brightness'         / from 0 to 1                                    
    PLANE2  = 'incidence'          / degrees                                        
    PLANE3  = 'emission'           / degrees                                        
    PLANE4  = 'phase   '           / degrees                                        
    PLANE5  = 'range   '           / kilometers                                     
    PLANE6  = 'facetX  '           / kilometers                                     
    PLANE7  = 'facetY  '           / kilometers                                     
    PLANE8  = 'facetZ  '           / kilometers                                     
    PLANE9  = 'normalX '           / X component of unit normal                     
    PLANE10 = 'normalY '           / Y component of unit normal                     
    PLANE11 = 'normalZ '           / Z component of unit normal                     
    MMFL    =             125.1963 / From SUM file                                  
    SCOBJ   = '{1.4378777439; 3.5947718256; 0.1647121385}' / From SUM file          
    CX      = '{-0.0219390994; -0.0187737183; 0.9995830248}' / From SUM file        
    CY      = '{0.9129225176; -0.407945255; 0.0123752087}' / From SUM file          
    CZ      = '{0.4075428232; 0.9128133525; 0.0260889017}' / From SUM file          
    SZ      = '{-0.4941045298; -0.8687927376; 0.0325559925}' / From SUM file        
    KMAT1   = '{117.647; 0; 0}'    / From SUM file                                  
    KMAT2   = '{0; -117.636; 0}'   / From SUM file                                  
    DIST    = '[0.0, 0.0, 0.0, 0.0]' / From SUM file                                
    SIGVSO  = '{1; 1; 1}'          / From SUM file                                  
    SIGPTG  = '{1; 1; 1}'          / From SUM file                                  
    END                                                                             

