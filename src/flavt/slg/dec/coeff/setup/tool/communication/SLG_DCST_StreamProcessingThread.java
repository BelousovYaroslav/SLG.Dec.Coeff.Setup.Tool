/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flavt.slg.dec.coeff.setup.tool.communication;

import flavt.slg.dec.coeff.setup.tool.main.SLG_DCST_App;
import flavt.slg.lib.constants.SLG_ConstantsParams;

/**
 *
 * @author yaroslav
 */
public class SLG_DCST_StreamProcessingThread implements Runnable {
    SLG_DCST_App theApp;
    static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SLG_DCST_StreamProcessingThread.class);

    public boolean m_bRunning;
    public boolean m_bStopThread;
    
    public SLG_DCST_StreamProcessingThread( SLG_DCST_App app) {
        theApp = app;
        
        m_bRunning = false;
        m_bStopThread = false;
    }
    
    @Override
    public void run() {
        m_bRunning = true;
        m_bStopThread = false;
        
        theApp.m_nMarkerFails = 0;
        theApp.m_nCounterFails = 0;
        theApp.m_nCheckSummFails = 0;
        theApp.m_nPacksCounter = 0;
        
    
        
        boolean bMarkerFailOnce = false;
        do {
            
            int nMarkerCounter = 0;
            do {
                if( theApp.m_bfCircleBuffer.getReadyIncomingDataLen() > 20) {
                    byte [] bts = new byte[1];
                    theApp.m_bfCircleBuffer.getAnswer( 1, bts);

                    String tmps = String.format( "BT: 0x%02X", bts[0]);
                    logger.trace( tmps);
                    
                    tmps = "BEFORE: " + nMarkerCounter;
                    switch( nMarkerCounter) {
                        case 0:
                            if( ( bts[0] & 0xFF) == 0x55)
                                nMarkerCounter++;
                            else
                                theApp.m_nMarkerFails++;
                        break;

                        case 1:
                            if( ( bts[0] & 0xFF) == 0xAA)
                                nMarkerCounter++;  //2! (условие выхода)
                            else {
                                nMarkerCounter = 0;
                                theApp.m_nMarkerFails++;
                            }
                        break;
                    }
                    tmps += " AFTER: " + nMarkerCounter;
                    logger.trace( tmps);
                }
                else {
                    if( m_bStopThread == true) {
                        return;
                    }
                }
            } while( nMarkerCounter != 2);
        
            if( theApp.m_bfCircleBuffer.getReadyIncomingDataLen() < 12) {
                logger.error( "После отмотки маркера в кольцевом буфере недостаточно байт пачки");
                continue;
            }
            
            byte [] bts = new byte[12];
            if( theApp.m_bfCircleBuffer.getAnswer( 12, bts) != 0) {
                logger.error( "После отмотки маркера кольцевой буфер не дал 12 байт!");
                continue;
            }
            
            //TODO: CHECKSUMM CHECK
            
            //logger.info(    String.format( "0x%02X", bts[4]));            
            
            //ANALYZE ADD.PARAM DESCRIPTOR
            switch( bts[4]) {
                case SLG_ConstantsParams.SLG_PARAM_VERSION:
                    logger.info(    String.format( "<< SLG_PARAM_VERSION: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],  bts[5],  bts[6],  bts[7],
                                        bts[8],  bts[9],  bts[10], bts[11]));
                    theApp.m_strVersion = String.format( "%d.%d.%d", ( bts[5] & 0xF0) >> 4, bts[5] & 0x0F, ( bts[6] & 0xF0) >> 4);
                    //logger.debug( "Получена версия ПО от прибора: " + theApp.m_strVersion);
                break;
                    
                case SLG_ConstantsParams.SLG_PARAM_PH_SH_CALIB_T:
                    logger.info(    String.format( "<< SLG_PARAM_PH_SH_CALIB_T: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],  bts[5],  bts[6],  bts[7],
                                        bts[8],  bts[9],  bts[10], bts[11]));
                    
                    if( bts[5] >= 0 && bts[5] < theApp.LIST_PARAMS_LEN) {
                        theApp.m_DevT[ bts[5]] = bts[6] & 0xFF;
                        if( theApp.m_DevT[ bts[5]] != 0xFF)
                            theApp.m_DevT[ bts[5]] -= 128;
                        theApp.m_bParamTDefined[ bts[5]] = true;
                    }
                    
                    logger.info( "" + theApp.m_DevT[0] +
                                " " + theApp.m_DevT[1] +
                                " " + theApp.m_DevT[2] +
                                " " + theApp.m_DevT[3] +
                                " " + theApp.m_DevT[4] +
                                " " + theApp.m_DevT[5] +
                                " " + theApp.m_DevT[6] +
                                " " + theApp.m_DevT[7] +
                                " " + theApp.m_DevT[8] +
                                " " + theApp.m_DevT[9] +
                                " " + theApp.m_DevT[10]);
                    
                    /*
                    logger.info( "" + theApp.m_bParamDefined[0] +
                                " " + theApp.m_bParamDefined[1] +
                                " " + theApp.m_bParamDefined[2] +
                                " " + theApp.m_bParamDefined[3] +
                                " " + theApp.m_bParamDefined[4] +
                                " " + theApp.m_bParamDefined[5] +
                                " " + theApp.m_bParamDefined[6] +
                                " " + theApp.m_bParamDefined[7] +
                                " " + theApp.m_bParamDefined[8] +
                                " " + theApp.m_bParamDefined[9] +
                                " " + theApp.m_bParamDefined[10]);
                    */
                break;
                    
                case SLG_ConstantsParams.SLG_PARAM_PH_SH_CALIB_PH_SH:
                    logger.info(    String.format( "<< SLG_PARAM_PH_SH_CALIB_PH_SH: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],  bts[5],  bts[6],  bts[7],
                                        bts[8],  bts[9],  bts[10], bts[11]));
                    
                    if( bts[5] >= 0 && bts[5] < theApp.LIST_PARAMS_LEN) {
                        theApp.m_DevPhsh[ bts[5]] = bts[6] & 0xFF;
                        theApp.m_bParamPhshDefined[ bts[5]] = true;
                    }
                    
                    logger.info( "" + theApp.m_DevPhsh[0] +
                                " " + theApp.m_DevPhsh[1] +
                                " " + theApp.m_DevPhsh[2] +
                                " " + theApp.m_DevPhsh[3] +
                                " " + theApp.m_DevPhsh[4] +
                                " " + theApp.m_DevPhsh[5] +
                                " " + theApp.m_DevPhsh[6] +
                                " " + theApp.m_DevPhsh[7] +
                                " " + theApp.m_DevPhsh[8] +
                                " " + theApp.m_DevPhsh[9] +
                                " " + theApp.m_DevPhsh[10]);
                    
                    /*
                    logger.info( "" + theApp.m_bParamDefined[0] +
                                " " + theApp.m_bParamDefined[1] +
                                " " + theApp.m_bParamDefined[2] +
                                " " + theApp.m_bParamDefined[3] +
                                " " + theApp.m_bParamDefined[4] +
                                " " + theApp.m_bParamDefined[5] +
                                " " + theApp.m_bParamDefined[6] +
                                " " + theApp.m_bParamDefined[7] +
                                " " + theApp.m_bParamDefined[8] +
                                " " + theApp.m_bParamDefined[9] +
                                " " + theApp.m_bParamDefined[10]);
                    */
                break;
                    
                case SLG_ConstantsParams.SLG_PARAM_PH_SH_USAGE:
                    logger.info(    String.format( "<< SLG_PARAM_PH_SH_USAGE: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],  bts[5],  bts[6],  bts[7],
                                        bts[8],  bts[9],  bts[10], bts[11]));
                    
                    if( bts[5] == 0x00) theApp.m_nPhShUsage = SLG_DCST_App.PHASE_SHIFT_USAGE_ON;
                    else                theApp.m_nPhShUsage = SLG_DCST_App.PHASE_SHIFT_USAGE_OFF;
                break;
                    
                case SLG_ConstantsParams.SLG_PARAM_PH_SH_CURRENT_VAL:
                    logger.info(    String.format( "<< SLG_PARAM_PH_SH_CURRENT_VAL: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],  bts[5],  bts[6],  bts[7],
                                        bts[8],  bts[9],  bts[10], bts[11]));
                    theApp.m_nCurrentPhaseShift = bts[5] & 0xFF;
                break;
                    
                case SLG_ConstantsParams.SLG_PARAM_UTD1:
                    //logger.info(    String.format( "<< SLG_PARAM_UTD1: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                    //                    bts[0],  bts[1],  bts[2],  bts[3],
                    //                    bts[4],  bts[5],  bts[6],  bts[7],
                    //                    bts[8],  bts[9],  bts[10], bts[11]));
                    
                    int nB6 = bts[6] & 0xFF;
                    int nB5 = bts[5] & 0xFF;
                    int nRes = ( nB6 << 8) + nB5;
                    theApp.m_dblTD1 =  ( ( double) nRes) / 65535. * 200. - 100.;
                break;
            }
            
            theApp.m_nPacksCounter = bts[9] & 0xFF;
            
        } while( m_bStopThread == false);
    }
}
