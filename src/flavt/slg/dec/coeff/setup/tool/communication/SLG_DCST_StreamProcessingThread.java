/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flavt.slg.dec.coeff.setup.tool.communication;

import flavt.slg.dec.coeff.setup.tool.main.SLG_DCST_App;
import flavt.slg.lib.constants.SLG_Constants;
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
            
            //ANALYZE DEVICE REGIME
            if( ( bts[10] & 0x20) > 0)
                theApp.m_nDeviceRegime = SLG_Constants.SLG_REGIME_ASYNC;
            else
                theApp.m_nDeviceRegime = SLG_Constants.SLG_REGIME_SYNC;
            
            //ANALYZE DEVICE MAIN PARAM OUTPUT
            if( ( bts[10] & 0x10) > 0) {
                theApp.m_nMainParamOutput = SLG_Constants.SLG_MAIN_PARAM_OUTPUT_DNDU;
                theApp.m_sh_dN = ( short) ( bts[ 1] * 256 + bts[ 0]);
                theApp.m_sh_dU = ( short) ( bts[ 3] * 256 + bts[ 2]);
                if( theApp.m_bDcCalculation) {
                    theApp.m_lSumm_dN += Math.abs( theApp.m_sh_dN);
                    theApp.m_lSumm_dU += Math.abs( theApp.m_sh_dU);
                    theApp.m_lDcCalcCounter++;
                }
            }
            else
                theApp.m_nMainParamOutput = SLG_Constants.SLG_MAIN_PARAM_OUTPUT_DPHI;
            
            //ANALYZE ADD.PARAM DESCRIPTOR
            int nB5, nB6, nRes;
            switch( bts[4]) {
                case SLG_ConstantsParams.SLG_PARAM_VERSION:
                    logger.info(    String.format( "<< SLG_PARAM_VERSION: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],  bts[5],  bts[6],  bts[7],
                                        bts[8],  bts[9],  bts[10], bts[11]));
                    theApp.m_strVersion = String.format( "%d.%d.%d", ( bts[5] & 0xF0) >> 4, bts[5] & 0x0F, ( bts[6] & 0xF0) >> 4);
                    //logger.debug( "Получена версия ПО от прибора: " + theApp.m_strVersion);
                break;
                    
                case SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T:
                    logger.info(    String.format( "<< SLG_PARAM_DC_CALIB_T: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
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
                    
                case SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L:
                    logger.info(    String.format( "<< SLG_PARAM_DC_CALIB_DC_L: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],
                                        bts[5],  bts[6],
                                        bts[7],  bts[8],  bts[9],  bts[10], bts[11]));
                    
                    if( bts[5] >= 0 && bts[5] < theApp.LIST_PARAMS_LEN) {
                        theApp.m_nDevDc[ bts[5]] =  ( theApp.m_nDevDc[ bts[5]] & 0xFF00) + ( bts[6] & 0xFF);
                        theApp.m_nParamDcDefined[ bts[5]] |= 0x01;
                    }
                    
                    logger.info( "" + theApp.m_nDevDc[0] +
                                " " + theApp.m_nDevDc[1] +
                                " " + theApp.m_nDevDc[2] +
                                " " + theApp.m_nDevDc[3] +
                                " " + theApp.m_nDevDc[4] +
                                " " + theApp.m_nDevDc[5] +
                                " " + theApp.m_nDevDc[6] +
                                " " + theApp.m_nDevDc[7] +
                                " " + theApp.m_nDevDc[8] +
                                " " + theApp.m_nDevDc[9] +
                                " " + theApp.m_nDevDc[10]);
                    
                    /*
                    logger.info( "" + theApp.m_nParamDcDefined[0] +
                                " " + theApp.m_nParamDcDefined[1] +
                                " " + theApp.m_nParamDcDefined[2] +
                                " " + theApp.m_nParamDcDefined[3] +
                                " " + theApp.m_nParamDcDefined[4] +
                                " " + theApp.m_nParamDcDefined[5] +
                                " " + theApp.m_nParamDcDefined[6] +
                                " " + theApp.m_nParamDcDefined[7] +
                                " " + theApp.m_nParamDcDefined[8] +
                                " " + theApp.m_nParamDcDefined[9] +
                                " " + theApp.m_nParamDcDefined[10]);
                    */
                break;
                    
                case SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H:
                    logger.info(    String.format( "<< SLG_PARAM_DC_CALIB_DC_H: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],  bts[5],  bts[6],  bts[7],
                                        bts[8],  bts[9],  bts[10], bts[11]));
                    
                    if( bts[5] >= 0 && bts[5] < theApp.LIST_PARAMS_LEN) {
                        theApp.m_nDevDc[ bts[5]] =  ( theApp.m_nDevDc[ bts[5]] & 0x00FF) + ( bts[6] << 8);
                        theApp.m_nParamDcDefined[ bts[5]] |= 0x02;
                    }
                    
                    logger.info( "" + theApp.m_nDevDc[0] +
                                " " + theApp.m_nDevDc[1] +
                                " " + theApp.m_nDevDc[2] +
                                " " + theApp.m_nDevDc[3] +
                                " " + theApp.m_nDevDc[4] +
                                " " + theApp.m_nDevDc[5] +
                                " " + theApp.m_nDevDc[6] +
                                " " + theApp.m_nDevDc[7] +
                                " " + theApp.m_nDevDc[8] +
                                " " + theApp.m_nDevDc[9] +
                                " " + theApp.m_nDevDc[10]);
                    
                    /*
                    logger.info( "" + theApp.m_nParamDcDefined[0] +
                                " " + theApp.m_nParamDcDefined[1] +
                                " " + theApp.m_nParamDcDefined[2] +
                                " " + theApp.m_nParamDcDefined[3] +
                                " " + theApp.m_nParamDcDefined[4] +
                                " " + theApp.m_nParamDcDefined[5] +
                                " " + theApp.m_nParamDcDefined[6] +
                                " " + theApp.m_nParamDcDefined[7] +
                                " " + theApp.m_nParamDcDefined[8] +
                                " " + theApp.m_nParamDcDefined[9] +
                                " " + theApp.m_nParamDcDefined[10]);
                    */
                break;                                       

                //075  Стартовый коэффициент вычета
                case SLG_ConstantsParams.SLG_PARAM_DC_START:
                    logger.info(    String.format( "<< SLG_PARAM_DC_START: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],
                                        bts[5],  bts[6],
                                        bts[7],  bts[8],  bts[9],  bts[10], bts[11]));
                    
                    nB6 = bts[6] & 0xFF;
                    nB5 = bts[5] & 0xFF;
                    theApp.m_nDecCoeffStart = ( nB6 << 8) + nB5;
                    if( theApp.m_nDecCoeffStart == 65535) theApp.m_nDecCoeffStart--;
                break;
                    
                case SLG_ConstantsParams.SLG_PARAM_DC_RECALC:
                    //076  Флаг как переопределять коэффициент вычета в процессе работы
                    //      0=перевычислять (как раньше)
                    //      1=калибровка ступенчатая
                    //      2=калибровка сглаженная
                    //      3=ручной режим
                    logger.info(    String.format( "<< SLG_PARAM_DC_RECALC: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],
                                        bts[5],  bts[6],
                                        bts[7],  bts[8],  bts[9],  bts[10], bts[11]));
                    
                    nB5 = bts[5] & 0xFF;
                    switch( nB5) {
                        case 0: theApp.m_nDecCoeffRecalc = SLG_DCST_App.DEC_COEFF_RECALC_RECALC; break;
                        case 1: theApp.m_nDecCoeffRecalc = SLG_DCST_App.DEC_COEFF_RECALC_CALIB_HARD; break;
                        case 2: theApp.m_nDecCoeffRecalc = SLG_DCST_App.DEC_COEFF_RECALC_CALIB_APPROX; break;
                        default: theApp.m_nDecCoeffRecalc = SLG_DCST_App.DEC_COEFF_RECALC_MANUAL; break;
                    }    
                            
                break;

                //077  Период переопределения Квычета (в секундах)
                case SLG_ConstantsParams.SLG_PARAM_DC_RECALC_PERIOD:
                    logger.info(    String.format( "<< SLG_PARAM_DC_RECALC_PERIOD: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],
                                        bts[5],  bts[6],
                                        bts[7],  bts[8],  bts[9],  bts[10], bts[11]));
                    
                    nB6 = bts[6] & 0xFF;
                    nB5 = bts[5] & 0xFF;
                    theApp.m_nDecCoeffRecalcPeriod = ( nB6 << 8) + nB5;
                    if( theApp.m_nDecCoeffRecalcPeriod > 600) theApp.m_nDecCoeffRecalcPeriod = 600;
                break;

                
                case SLG_ConstantsParams.SLG_PARAM_DC_START_DEF:
                    //066  Настройки использования коэффициента вычета. Что брать при старте:
                    //      0x00    = DC_START
                    //      REST    = таблица калибровки)
                    logger.info(    String.format( "<< SLG_PARAM_DC_START_DEF: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],
                                        bts[5],  bts[6],
                                        bts[7],  bts[8],  bts[9],  bts[10], bts[11]));
                    
                    nB5 = bts[5] & 0xFF;
                    if( nB5 == 1)
                        theApp.m_nDecCoeffStartDef = SLG_DCST_App.DEC_COEFF_STARTDEF_CALIB;
                    else
                        theApp.m_nDecCoeffStartDef = SLG_DCST_App.DEC_COEFF_STARTDEF_DCSTART;
                break;
                    
                case SLG_ConstantsParams.SLG_PARAM_DEC_COEFF:  //ТЕКУЩИЙ
                    logger.info(    String.format( "<< SLG_PARAM_DEC_COEFF: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                                        bts[0],  bts[1],  bts[2],  bts[3],
                                        bts[4],  bts[5],  bts[6],  bts[7],
                                        bts[8],  bts[9],  bts[10], bts[11]));
                    nB6 = bts[6] & 0xFF;
                    nB5 = bts[5] & 0xFF;
                    theApp.m_nDecCoeffCurrent = ( nB6 << 8) + nB5;
                    if( theApp.m_nDecCoeffCurrent == 65535) theApp.m_nDecCoeffCurrent--;
                break;
                    
                case SLG_ConstantsParams.SLG_PARAM_UTD1:
                    //logger.info(    String.format( "<< SLG_PARAM_UTD1: %02X %02X %02X %02X   %02X   %02X %02X   %02X %02X   %02X   %02X   %02X",
                    //                    bts[0],  bts[1],  bts[2],  bts[3],
                    //                    bts[4],  bts[5],  bts[6],  bts[7],
                    //                    bts[8],  bts[9],  bts[10], bts[11]));
                    
                    nB6 = bts[6] & 0xFF;
                    nB5 = bts[5] & 0xFF;
                    nRes = ( nB6 << 8) + nB5;
                    theApp.m_dblTD1 =  ( ( double) nRes) / 65535. * 200. - 100.;
                break;
            }
            
            theApp.m_nPacksCounter = bts[9] & 0xFF;
            
        } while( m_bStopThread == false);
    }
}
