/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flavt.slg.dec.coeff.setup.tool.main;

import flavt.slg.lib.constants.SLG_ConstantsCmd;
import flavt.slg.lib.constants.SLG_ConstantsParams;
import flavt.slg.dec.coeff.setup.tool.communication.SLG_DCST_CircleBuffer;
import flavt.slg.dec.coeff.setup.tool.communication.SLG_DCST_StreamProcessingThread;
import flavt.slg.lib.constants.SLG_Constants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.apache.log4j.Logger;

/**
 *
 * @author yaroslav
 */
public class SLG_DCST_MainFrame extends javax.swing.JFrame {
    static Logger logger = Logger.getLogger(SLG_DCST_MainFrame.class);
    private final SLG_DCST_App theApp;
    
    Timer tRefreshStates;
    Timer tRefreshValues;
    Timer tPolling;
    
    public String m_strPort;
    public static SerialPort serialPort;
    PortReader m_evListener;
    
    LinkedList m_lstRequestedParams;
    Iterator m_itRequestedParams;
    SLG_DCST_StreamProcessingThread thrProcessorRunnable;
    Thread thrProcessorThread;
    
    byte m_btValueH;            ///FUCK FAKE
    
    /**
     * Creates new form MainFrame
     */
    public SLG_DCST_MainFrame( SLG_DCST_App app) {
        
        class ReqItem {
            private final byte m_nParamIndex;
            private final byte m_nParamSubIndex;
            
            public ReqItem( byte Indx, byte SubIndx) {
                m_nParamIndex = Indx;
                m_nParamSubIndex = SubIndx;
            }
        }
        
        theApp = app;
        initComponents();
        
        m_lstRequestedParams = new LinkedList();
        m_lstRequestedParams.add( new ReqItem( ( byte) SLG_ConstantsParams.SLG_PARAM_DC_START_DEF, ( byte) 0) );
        m_lstRequestedParams.add( new ReqItem( ( byte) SLG_ConstantsParams.SLG_PARAM_DC_START, ( byte) 0) );
        m_lstRequestedParams.add( new ReqItem( ( byte) SLG_ConstantsParams.SLG_PARAM_DC_RECALC, ( byte) 0) );
        m_lstRequestedParams.add( new ReqItem( ( byte) SLG_ConstantsParams.SLG_PARAM_DC_RECALC_PERIOD, ( byte) 0) );
        
        m_lstRequestedParams.add( new ReqItem( ( byte) SLG_ConstantsParams.SLG_PARAM_DEC_COEFF, ( byte) 0));
        for( int i=0; i < theApp.LIST_PARAMS_LEN; i++) {
            m_lstRequestedParams.add( new ReqItem( ( byte) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) i));
            m_lstRequestedParams.add( new ReqItem( ( byte) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) i));
        }
        m_itRequestedParams = m_lstRequestedParams.iterator();

        edtComPortValue.setText( theApp.GetSettings().GetComPort());
        
        theApp.m_bfCircleBuffer= new SLG_DCST_CircleBuffer();
        
        thrProcessorRunnable = new SLG_DCST_StreamProcessingThread( theApp);
        thrProcessorThread = new Thread( thrProcessorRunnable);
        thrProcessorThread.start();
        
        
        tRefreshStates = new Timer( 200, new ActionListener() {

            @Override
            public void actionPerformed( ActionEvent e) {
                
                btnConnect.setEnabled( !theApp.m_bConnected);
                btnDisconnect.setEnabled( theApp.m_bConnected);
                
                
                boolean bAllDefined = true;
                for( int i = 0; i < theApp.LIST_PARAMS_LEN; bAllDefined = bAllDefined & theApp.m_bParamTDefined[i] & (theApp.m_nParamDcDefined[i++] == 0x03));
                
                
                btnDecCoeffRecalcCalibApprox.setEnabled( theApp.m_bConnected && bAllDefined);
                btnDecCoeffRecalcRecalc.setEnabled( theApp.m_bConnected && bAllDefined);
                btnDecCoeffRecalcManual.setEnabled( theApp.m_bConnected && bAllDefined);
                
                btnResetCalibData.setEnabled( theApp.m_bConnected && bAllDefined);
                btnSaveData.setEnabled( theApp.m_bConnected && bAllDefined);
                
                JButton btnsTGet[] = { btnT1Get, btnT2Get, btnT3Get, btnT4Get,
                                       btnT5Get, btnT6Get, btnT7Get, btnT8Get,
                                       btnT9Get, btnT10Get, btnT11Get};
                
                JButton btnsTSet[] = { btnT1Set, btnT2Set, btnT3Set, btnT4Set,
                                       btnT5Set, btnT6Set, btnT7Set, btnT8Set,
                                       btnT9Set, btnT10Set, btnT11Set};
                
                JButton btnsPhshGet[] = { btnPS1Get, btnPS2Get, btnPS3Get, btnPS4Get,
                                          btnPS5Get, btnPS6Get, btnPS7Get, btnPS8Get,
                                          btnPS9Get, btnPS10Get, btnPS11Get};
                
                JButton btnsPhshSet[] = { btnPS1Set, btnPS2Set, btnPS3Set, btnPS4Set,
                                          btnPS5Set, btnPS6Set, btnPS7Set, btnPS8Set,
                                          btnPS9Set, btnPS10Set, btnPS11Set};
                        
                for( int i=0; i<11; i++) {
                    btnsTGet[i].setEnabled( theApp.m_bConnected && bAllDefined);
                    btnsTSet[i].setEnabled( theApp.m_bConnected && bAllDefined &&
                                theApp.m_nDecCoeffRecalc != SLG_DCST_App.DEC_COEFF_RECALC_CALIB_HARD &&
                                theApp.m_nDecCoeffRecalc != SLG_DCST_App.DEC_COEFF_RECALC_CALIB_APPROX);
                    btnsPhshGet[i].setEnabled( theApp.m_bConnected && bAllDefined);
                    btnsPhshSet[i].setEnabled( theApp.m_bConnected && bAllDefined &&
                                theApp.m_nDecCoeffRecalc != SLG_DCST_App.DEC_COEFF_RECALC_CALIB_HARD &&
                                theApp.m_nDecCoeffRecalc != SLG_DCST_App.DEC_COEFF_RECALC_CALIB_APPROX);
                }

            }
        });
        tRefreshStates.start();
        
        tRefreshValues = new Timer( 200, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                
                if( theApp.m_bConnected) {
                    String strStatus = "";
                    
                    if( !theApp.m_strVersion.isEmpty())
                        strStatus = "  Версия ПО прибора = " + theApp.m_strVersion;
                    
                    strStatus +=
                            String.format( "  MF:%d CF:%d CSF:%d PC:%d",
                                    theApp.m_nMarkerFails,
                                    theApp.m_nCounterFails,
                                    theApp.m_nCheckSummFails,
                                    theApp.m_nPacksCounter);
                    
                    
                    
                    lblConnectionStateValue.setText( strStatus);
                }
                else {
                    lblConnectionStateValue.setText( "  Нет соединения");
                }
                    
                /*
                if( theApp.m_bConnected) {
                    switch( theApp.m_nDecCoeffCalibrationUsage) {
                        case SLG_DCST_App.DEC_COEFF_CALIBRATION_USAGE_CALIB:
                            lblPhaseShiftUsageValue.setText( "Калибровка"); break;
                        case SLG_DCST_App.DEC_COEFF_CALIBRATION_USAGE_MANUAL:
                            lblPhaseShiftUsageValue.setText( "Ручной"); break;
                        case SLG_DCST_App.DEC_COEFF_CALIBRATION_USAGE_RECALC:
                            lblPhaseShiftUsageValue.setText( "Перевычисление"); break;
                        case SLG_DCST_App.DEC_COEFF_CALIBRATION_USAGE_OFF:
                            lblPhaseShiftUsageValue.setText( "Выключено"); break;
                        case SLG_DCST_App.DEC_COEFF_CALIBRATION_USAGE_UNKNOWN:
                            lblPhaseShiftUsageValue.setText( "НЕИЗВ"); break;
                        default:
                            logger.warn( "Странное значение использования коэффициента вычета!");
                            lblPhaseShiftUsageValue.setText( "????"); break;
                
                    }
                }
                else {
                    lblPhaseShiftUsageValue.setText( "XXX");
                }
                */
                
                if( theApp.m_bConnected) {
                    if( theApp.m_nDeviceRegime == SLG_Constants.SLG_REGIME_SYNC)
                        lblCurrentDecCoeffValue.setText( "СИНХ");
                    else
                        lblCurrentDecCoeffValue.setText( String.format( "%.05f", ( double) theApp.m_nDecCoeffCurrent / 655350.));
                        
                }
                else {
                    lblCurrentDecCoeffValue.setText( "XXX");
                }
                
                if( theApp.m_bConnected) {
                    lblCurrentTD1Value.setText( String.format( "%.2f", theApp.m_dblTD1));
                }
                else {
                    lblCurrentTD1Value.setText( "XXX");
                }
                

                
                if( theApp.m_bConnected) {
                    if( theApp.m_nDecCoeffStartDef == SLG_DCST_App.DEC_COEFF_STARTDEF_DCSTART)
                        lblDcStartDcStart.setText( "v");
                    else
                        lblDcStartDcStart.setText( " ");
                    
                    if( theApp.m_nDecCoeffStartDef == SLG_DCST_App.DEC_COEFF_STARTDEF_CALIB)
                        lblDcStartTable.setText( "v");
                    else
                        lblDcStartTable.setText( " ");
                    
                }
                else {
                    lblDcStartDcStart.setText( "x");
                    lblDcStartTable.setText( "x");
                }
                
                
                if( theApp.m_bConnected) {
                    if( theApp.m_nDecCoeffStart == 65535)
                        edtDcStartCurrentValue.setText( "");
                    else
                        edtDcStartCurrentValue.setText( String.format( "%.6f", ( double) theApp.m_nDecCoeffStart / 655350.));
                    
                }
                else {
                    edtDcStartCurrentValue.setText( "x");
                }
                
                
                
                if( theApp.m_bConnected) {
                    if( theApp.m_nDecCoeffRecalc == SLG_DCST_App.DEC_COEFF_RECALC_RECALC)
                        lblSignDecCoeffRecalcRecalc.setText( "v");
                    else
                        lblSignDecCoeffRecalcRecalc.setText( " ");
                    
                    if( theApp.m_nDecCoeffRecalc == SLG_DCST_App.DEC_COEFF_RECALC_CALIB_HARD)
                        lblSignDecCoeffRecalcCalibHard.setText( "v");
                    else
                        lblSignDecCoeffRecalcCalibHard.setText( " ");
                    
                    if( theApp.m_nDecCoeffRecalc == SLG_DCST_App.DEC_COEFF_RECALC_CALIB_APPROX)
                        lblSignDecCoeffRecalcCalibApprox.setText( "v");
                    else
                        lblSignDecCoeffRecalcCalibApprox.setText( " ");
                    
                    if( theApp.m_nDecCoeffRecalc == SLG_DCST_App.DEC_COEFF_RECALC_MANUAL)
                        lblSignDecCoeffRecalcManual.setText( "v");
                    else
                        lblSignDecCoeffRecalcManual.setText( " ");                    
                }
                else {
                    lblSignDecCoeffRecalcRecalc.setText( "x");
                    lblSignDecCoeffRecalcCalibHard.setText( "x");
                    lblSignDecCoeffRecalcCalibApprox.setText( "x");
                    lblSignDecCoeffRecalcManual.setText( "x");
                }
                
                if( theApp.m_bConnected) {
                    if( theApp.m_nDecCoeffRecalcPeriod != 65535)
                        edtDcRecalcPeriodCurrent.setText( "" + theApp.m_nDecCoeffRecalcPeriod);
                    else
                        edtDcRecalcPeriodCurrent.setText( "");
                }
                else
                    edtDcRecalcPeriodCurrent.setText( "x");
                
                //панель перевычисления dc
                if( theApp.m_bConnected) {
                    
                    switch( theApp.m_nMainParamOutput) {
                        case SLG_Constants.SLG_MAIN_PARAM_OUTPUT_DPHI:
                            lblCurrentoutputParam.setText( "Прибор выдаёт приращения угла");
                        break;
                            
                        case SLG_Constants.SLG_MAIN_PARAM_OUTPUT_DNDU:
                            lblCurrentoutputParam.setText( "Прибор выдаёт dN-dU");
                        break;
                            
                        default:
                            lblCurrentoutputParam.setText( "Основной параметр выдаваемый прибором неопределён");
                    }
                    
                    lblDcCalc_N.setText(    String.format( "N=%05d", theApp.m_lDcCalcCounter));
                    lblDcCalc_dN.setText(   String.format( "dN=%+05d", theApp.m_sh_dN));
                    lblDcCalc_SdN.setText(  String.format( "ΣdN=%05d", theApp.m_lSumm_dN));
                    lblDcCalc_dU.setText(   String.format( "dU=%+05d", theApp.m_sh_dU));
                    lblDcCalc_SdU.setText(  String.format( "ΣdU=%05d", theApp.m_lSumm_dU));
                    lblDcCalc_DC.setText(   String.format( "DC=%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
                }
                else {
                    lblCurrentoutputParam.setText( "Нет связи с прибором");
                    lblDcCalc_N.setText( "");
                    lblDcCalc_dN.setText( "");
                    lblDcCalc_SdN.setText( "");
                    lblDcCalc_dU.setText( "");
                    lblDcCalc_SdU.setText( "");
                    lblDcCalc_DC.setText( "");
                }

                
                JTextField edtsT[] =  { edtT1Show, edtT2Show, edtT3Show, edtT4Show, edtT5Show,
                                        edtT6Show, edtT7Show, edtT8Show, edtT9Show, edtT10Show,
                                        edtT11Show };
                
                JTextField edtsPS[] = { edtPS1Show, edtPS2Show, edtPS3Show, edtPS4Show, edtPS5Show,
                                        edtPS6Show, edtPS7Show, edtPS8Show, edtPS9Show, edtPS10Show,
                                        edtPS11Show };
                    
                for( int i = 0; i < theApp.LIST_PARAMS_LEN; i++) {
                    //T
                    if( theApp.m_bParamTDefined[i] == true) {
                        if( theApp.m_DevT[i] == 0xFFFF)
                            edtsT[i].setText( "---");
                        else
                            edtsT[i].setText( String.format( "%d", theApp.m_DevT[ i]));
                            
                    }
                    else
                        edtsT[i].setText( "???");
                    
                    
                    //dc
                    if( theApp.m_nParamDcDefined[i] == 0x03) {
                        if( theApp.m_nDevDc[i] == 0xFFFF)
                            edtsPS[i].setText( "---");
                        else {
                            double dblVal = ( double) ( theApp.m_nDevDc[ i] & 0xFFFF ) / 65535.;
                            edtsPS[i].setText( String.format( "%.05f", dblVal));
                        }
                            
                    }
                    else
                        edtsPS[i].setText( "???");
                }
            }
            
            
        });
        tRefreshValues.start();
        
        tPolling = new Timer( 200, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if( theApp.m_bConnected && serialPort != null && serialPort.isOpened()) {
                    
                    if( theApp.m_strVersion.isEmpty()) {
                        SendComandRequestParam( ( byte) SLG_ConstantsParams.SLG_PARAM_VERSION, ( byte) 0);
                    }
                    else {
                        if( m_itRequestedParams.hasNext() == false)
                            m_itRequestedParams = m_lstRequestedParams.iterator();
                        
                        ReqItem item = ( ReqItem) m_itRequestedParams.next();
                        SendComandRequestParam( item.m_nParamIndex, item.m_nParamSubIndex);
                    }
                }
            }
            
        });
        tPolling.start();
    }

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblPort = new javax.swing.JLabel();
        edtComPortValue = new javax.swing.JTextField();
        btnConnect = new javax.swing.JButton();
        btnDisconnect = new javax.swing.JButton();
        lblConnectionStateTitle = new javax.swing.JLabel();
        lblConnectionStateValue = new javax.swing.JLabel();
        pnlCurrentParams = new javax.swing.JPanel();
        lblCurrentDecCoeffTitle = new javax.swing.JLabel();
        lblCurrentTD1Title = new javax.swing.JLabel();
        lblCurrentDecCoeffValue = new javax.swing.JLabel();
        lblCurrentTD1Value = new javax.swing.JLabel();
        pnlStartParameters = new javax.swing.JPanel();
        btnDcStartSetTable = new javax.swing.JButton();
        btnSaveDcStartDefAndValue = new javax.swing.JButton();
        lblDcStartTable = new javax.swing.JLabel();
        lblDcStartDcStart = new javax.swing.JLabel();
        edtDcStartCurrentValue = new javax.swing.JTextField();
        edtDcStartSetValue = new javax.swing.JTextField();
        btnSetDcStartValue = new javax.swing.JButton();
        btnDcStartSetDcStart = new javax.swing.JButton();
        pnlInProcess = new javax.swing.JPanel();
        lblSignDecCoeffRecalcRecalc = new javax.swing.JLabel();
        btnDecCoeffRecalcRecalc = new javax.swing.JButton();
        lblSignDecCoeffRecalcCalibHard = new javax.swing.JLabel();
        btnDecCoeffRecalcCalibHard = new javax.swing.JButton();
        lblSignDecCoeffRecalcCalibApprox = new javax.swing.JLabel();
        btnDecCoeffRecalcCalibApprox = new javax.swing.JButton();
        lblSignDecCoeffRecalcManual = new javax.swing.JLabel();
        btnDecCoeffRecalcManual = new javax.swing.JButton();
        lblDecCoeffRecalcTitle = new javax.swing.JLabel();
        lblDecCoeffRecalcPeriodValuePrefix = new javax.swing.JLabel();
        edtDcRecalcPeriodCurrent = new javax.swing.JTextField();
        edtDcRecalcPeriodSet = new javax.swing.JTextField();
        lblDecCoeffRecalcUnits = new javax.swing.JLabel();
        btnDecCoeffRecalсSave = new javax.swing.JButton();
        btnSetDcRecalcPeriod = new javax.swing.JButton();
        pnlCalibrationTable = new javax.swing.JPanel();
        lblTemperature = new javax.swing.JLabel();
        lblPhaseShift = new javax.swing.JLabel();
        btnT1Get = new javax.swing.JButton();
        edtT1Show = new javax.swing.JTextField();
        edtT1Edit = new javax.swing.JTextField();
        btnT1Set = new javax.swing.JButton();
        btnPS1Get = new javax.swing.JButton();
        edtPS1Show = new javax.swing.JTextField();
        edtPS1Edit = new javax.swing.JTextField();
        btnPS1Set = new javax.swing.JButton();
        btnT2Get = new javax.swing.JButton();
        edtT2Show = new javax.swing.JTextField();
        edtT2Edit = new javax.swing.JTextField();
        btnT2Set = new javax.swing.JButton();
        btnPS2Get = new javax.swing.JButton();
        edtPS2Show = new javax.swing.JTextField();
        edtPS2Edit = new javax.swing.JTextField();
        btnPS2Set = new javax.swing.JButton();
        btnT3Get = new javax.swing.JButton();
        edtT3Show = new javax.swing.JTextField();
        edtT3Edit = new javax.swing.JTextField();
        btnT3Set = new javax.swing.JButton();
        btnPS3Get = new javax.swing.JButton();
        edtPS3Show = new javax.swing.JTextField();
        edtPS3Edit = new javax.swing.JTextField();
        btnPS3Set = new javax.swing.JButton();
        btnT4Get = new javax.swing.JButton();
        edtT4Show = new javax.swing.JTextField();
        edtT4Edit = new javax.swing.JTextField();
        btnT4Set = new javax.swing.JButton();
        btnT5Get = new javax.swing.JButton();
        edtT5Show = new javax.swing.JTextField();
        edtT5Edit = new javax.swing.JTextField();
        btnT5Set = new javax.swing.JButton();
        btnPS4Get = new javax.swing.JButton();
        edtPS4Show = new javax.swing.JTextField();
        edtPS4Edit = new javax.swing.JTextField();
        btnPS4Set = new javax.swing.JButton();
        btnPS5Get = new javax.swing.JButton();
        edtPS5Show = new javax.swing.JTextField();
        edtPS5Edit = new javax.swing.JTextField();
        btnPS5Set = new javax.swing.JButton();
        btnT6Get = new javax.swing.JButton();
        edtT6Show = new javax.swing.JTextField();
        edtT6Edit = new javax.swing.JTextField();
        btnT6Set = new javax.swing.JButton();
        btnT7Get = new javax.swing.JButton();
        edtT7Show = new javax.swing.JTextField();
        edtT7Edit = new javax.swing.JTextField();
        btnT7Set = new javax.swing.JButton();
        btnT8Get = new javax.swing.JButton();
        edtT8Show = new javax.swing.JTextField();
        edtT8Edit = new javax.swing.JTextField();
        btnT8Set = new javax.swing.JButton();
        btnT9Get = new javax.swing.JButton();
        edtT9Show = new javax.swing.JTextField();
        edtT9Edit = new javax.swing.JTextField();
        btnT9Set = new javax.swing.JButton();
        btnT10Get = new javax.swing.JButton();
        edtT10Show = new javax.swing.JTextField();
        edtT10Edit = new javax.swing.JTextField();
        btnT10Set = new javax.swing.JButton();
        btnT11Get = new javax.swing.JButton();
        edtT11Show = new javax.swing.JTextField();
        edtT11Edit = new javax.swing.JTextField();
        btnT11Set = new javax.swing.JButton();
        btnPS6Get = new javax.swing.JButton();
        edtPS6Show = new javax.swing.JTextField();
        edtPS6Edit = new javax.swing.JTextField();
        btnPS6Set = new javax.swing.JButton();
        btnPS7Get = new javax.swing.JButton();
        edtPS7Show = new javax.swing.JTextField();
        edtPS7Edit = new javax.swing.JTextField();
        btnPS7Set = new javax.swing.JButton();
        btnPS8Get = new javax.swing.JButton();
        edtPS8Show = new javax.swing.JTextField();
        edtPS8Edit = new javax.swing.JTextField();
        btnPS8Set = new javax.swing.JButton();
        btnPS9Get = new javax.swing.JButton();
        edtPS9Show = new javax.swing.JTextField();
        edtPS9Edit = new javax.swing.JTextField();
        btnPS9Set = new javax.swing.JButton();
        btnPS10Get = new javax.swing.JButton();
        edtPS10Show = new javax.swing.JTextField();
        edtPS10Edit = new javax.swing.JTextField();
        btnPS10Set = new javax.swing.JButton();
        btnPS11Get = new javax.swing.JButton();
        edtPS11Show = new javax.swing.JTextField();
        edtPS11Edit = new javax.swing.JTextField();
        btnPS11Set = new javax.swing.JButton();
        btnResetCalibData = new javax.swing.JButton();
        btnSaveData = new javax.swing.JButton();
        pnlCalcDc = new javax.swing.JPanel();
        lblCurrentoutputParam = new javax.swing.JLabel();
        btnSwitchCurrentOutputParam = new javax.swing.JButton();
        btnCalcDcStart = new javax.swing.JButton();
        SetCalcedDcAsStartValue = new javax.swing.JButton();
        btnCalcDcReset = new javax.swing.JButton();
        btnT1 = new javax.swing.JButton();
        btnT2 = new javax.swing.JButton();
        btnT3 = new javax.swing.JButton();
        btnT4 = new javax.swing.JButton();
        btnT5 = new javax.swing.JButton();
        btnT6 = new javax.swing.JButton();
        btnT7 = new javax.swing.JButton();
        btnT8 = new javax.swing.JButton();
        btnT9 = new javax.swing.JButton();
        btnT10 = new javax.swing.JButton();
        btnT11 = new javax.swing.JButton();
        lblDcCalc_dN = new javax.swing.JLabel();
        lblDcCalc_N = new javax.swing.JLabel();
        lblDcCalc_dU = new javax.swing.JLabel();
        lblDcCalc_SdU = new javax.swing.JLabel();
        lblDcCalc_SdN = new javax.swing.JLabel();
        lblDcCalc_DC = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("МЛГ3Б. Утилита для редактирования калибровки коэффициента вычета  (С) ФЛАВТ   2017.11.20 16:45");
        setMinimumSize(new java.awt.Dimension(1270, 720));
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(null);

        lblPort.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblPort.setText("<html><b><u>Порт</b></u></html>");
        getContentPane().add(lblPort);
        lblPort.setBounds(20, 10, 50, 30);
        getContentPane().add(edtComPortValue);
        edtComPortValue.setBounds(70, 10, 220, 30);

        btnConnect.setText("Соединить");
        btnConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectActionPerformed(evt);
            }
        });
        getContentPane().add(btnConnect);
        btnConnect.setBounds(300, 10, 130, 30);

        btnDisconnect.setText("Разъединить");
        btnDisconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDisconnectActionPerformed(evt);
            }
        });
        getContentPane().add(btnDisconnect);
        btnDisconnect.setBounds(440, 10, 130, 30);

        lblConnectionStateTitle.setText("Состояние связи:");
        getContentPane().add(lblConnectionStateTitle);
        lblConnectionStateTitle.setBounds(20, 50, 130, 30);

        lblConnectionStateValue.setText("jLabel2");
        lblConnectionStateValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        getContentPane().add(lblConnectionStateValue);
        lblConnectionStateValue.setBounds(150, 50, 420, 30);

        pnlCurrentParams.setBorder(javax.swing.BorderFactory.createTitledBorder("Текущие параметры"));
        pnlCurrentParams.setLayout(null);

        lblCurrentDecCoeffTitle.setText("Текущее (последнее выставленное) значение Квычета: ");
        pnlCurrentParams.add(lblCurrentDecCoeffTitle);
        lblCurrentDecCoeffTitle.setBounds(10, 20, 410, 30);

        lblCurrentTD1Title.setText("Текущая температура (TD1):");
        pnlCurrentParams.add(lblCurrentTD1Title);
        lblCurrentTD1Title.setBounds(10, 60, 410, 30);

        lblCurrentDecCoeffValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCurrentDecCoeffValue.setText("???");
        lblCurrentDecCoeffValue.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pnlCurrentParams.add(lblCurrentDecCoeffValue);
        lblCurrentDecCoeffValue.setBounds(430, 20, 130, 30);

        lblCurrentTD1Value.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCurrentTD1Value.setText("???");
        lblCurrentTD1Value.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pnlCurrentParams.add(lblCurrentTD1Value);
        lblCurrentTD1Value.setBounds(430, 60, 130, 30);

        getContentPane().add(pnlCurrentParams);
        pnlCurrentParams.setBounds(10, 90, 570, 100);

        pnlStartParameters.setBorder(javax.swing.BorderFactory.createTitledBorder("Стартовое значение коэффициента вычета"));
        pnlStartParameters.setLayout(null);

        btnDcStartSetTable.setText("Брать из таблицы калибровки");
        btnDcStartSetTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDcStartSetTableActionPerformed(evt);
            }
        });
        pnlStartParameters.add(btnDcStartSetTable);
        btnDcStartSetTable.setBounds(50, 60, 260, 30);

        btnSaveDcStartDefAndValue.setText("Сохранить");
        btnSaveDcStartDefAndValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveDcStartDefAndValueActionPerformed(evt);
            }
        });
        pnlStartParameters.add(btnSaveDcStartDefAndValue);
        btnSaveDcStartDefAndValue.setBounds(430, 60, 130, 30);

        lblDcStartTable.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDcStartTable.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
        pnlStartParameters.add(lblDcStartTable);
        lblDcStartTable.setBounds(10, 60, 30, 30);

        lblDcStartDcStart.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDcStartDcStart.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
        pnlStartParameters.add(lblDcStartDcStart);
        lblDcStartDcStart.setBounds(10, 20, 30, 30);

        edtDcStartCurrentValue.setEditable(false);
        edtDcStartCurrentValue.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtDcStartCurrentValue.setEnabled(false);
        pnlStartParameters.add(edtDcStartCurrentValue);
        edtDcStartCurrentValue.setBounds(320, 20, 80, 30);

        edtDcStartSetValue.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlStartParameters.add(edtDcStartSetValue);
        edtDcStartSetValue.setBounds(410, 20, 80, 30);

        btnSetDcStartValue.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnSetDcStartValue.setText("set");
        btnSetDcStartValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetDcStartValueActionPerformed(evt);
            }
        });
        pnlStartParameters.add(btnSetDcStartValue);
        btnSetDcStartValue.setBounds(500, 20, 60, 30);

        btnDcStartSetDcStart.setText("Стартовое значение");
        btnDcStartSetDcStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDcStartSetDcStartActionPerformed(evt);
            }
        });
        pnlStartParameters.add(btnDcStartSetDcStart);
        btnDcStartSetDcStart.setBounds(50, 20, 260, 30);

        getContentPane().add(pnlStartParameters);
        pnlStartParameters.setBounds(10, 200, 570, 100);

        pnlInProcess.setBorder(javax.swing.BorderFactory.createTitledBorder("Переопределение коэффициента вычета в процессе работы"));
        pnlInProcess.setLayout(null);

        lblSignDecCoeffRecalcRecalc.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSignDecCoeffRecalcRecalc.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
        pnlInProcess.add(lblSignDecCoeffRecalcRecalc);
        lblSignDecCoeffRecalcRecalc.setBounds(10, 20, 30, 30);

        btnDecCoeffRecalcRecalc.setText("Перевычисление");
        btnDecCoeffRecalcRecalc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDecCoeffRecalcRecalcActionPerformed(evt);
            }
        });
        pnlInProcess.add(btnDecCoeffRecalcRecalc);
        btnDecCoeffRecalcRecalc.setBounds(50, 20, 260, 30);

        lblSignDecCoeffRecalcCalibHard.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSignDecCoeffRecalcCalibHard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
        pnlInProcess.add(lblSignDecCoeffRecalcCalibHard);
        lblSignDecCoeffRecalcCalibHard.setBounds(10, 60, 30, 30);

        btnDecCoeffRecalcCalibHard.setText("Калибровка ступенчатая");
        btnDecCoeffRecalcCalibHard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDecCoeffRecalcCalibHardActionPerformed(evt);
            }
        });
        pnlInProcess.add(btnDecCoeffRecalcCalibHard);
        btnDecCoeffRecalcCalibHard.setBounds(50, 60, 260, 30);

        lblSignDecCoeffRecalcCalibApprox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSignDecCoeffRecalcCalibApprox.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
        pnlInProcess.add(lblSignDecCoeffRecalcCalibApprox);
        lblSignDecCoeffRecalcCalibApprox.setBounds(10, 100, 30, 30);

        btnDecCoeffRecalcCalibApprox.setText("Калибровка сглаженная");
        btnDecCoeffRecalcCalibApprox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDecCoeffRecalcCalibApproxActionPerformed(evt);
            }
        });
        pnlInProcess.add(btnDecCoeffRecalcCalibApprox);
        btnDecCoeffRecalcCalibApprox.setBounds(50, 100, 260, 30);

        lblSignDecCoeffRecalcManual.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSignDecCoeffRecalcManual.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
        pnlInProcess.add(lblSignDecCoeffRecalcManual);
        lblSignDecCoeffRecalcManual.setBounds(10, 140, 30, 30);

        btnDecCoeffRecalcManual.setText("Ручной режим");
        btnDecCoeffRecalcManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDecCoeffRecalcManualActionPerformed(evt);
            }
        });
        pnlInProcess.add(btnDecCoeffRecalcManual);
        btnDecCoeffRecalcManual.setBounds(50, 140, 260, 30);

        lblDecCoeffRecalcTitle.setText("Частота переопределения");
        pnlInProcess.add(lblDecCoeffRecalcTitle);
        lblDecCoeffRecalcTitle.setBounds(320, 40, 240, 30);

        lblDecCoeffRecalcPeriodValuePrefix.setText("раз в");
        pnlInProcess.add(lblDecCoeffRecalcPeriodValuePrefix);
        lblDecCoeffRecalcPeriodValuePrefix.setBounds(320, 70, 40, 30);

        edtDcRecalcPeriodCurrent.setEditable(false);
        edtDcRecalcPeriodCurrent.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtDcRecalcPeriodCurrent.setEnabled(false);
        pnlInProcess.add(edtDcRecalcPeriodCurrent);
        edtDcRecalcPeriodCurrent.setBounds(360, 70, 50, 30);
        pnlInProcess.add(edtDcRecalcPeriodSet);
        edtDcRecalcPeriodSet.setBounds(420, 70, 50, 30);

        lblDecCoeffRecalcUnits.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDecCoeffRecalcUnits.setText("сек");
        pnlInProcess.add(lblDecCoeffRecalcUnits);
        lblDecCoeffRecalcUnits.setBounds(470, 70, 30, 30);

        btnDecCoeffRecalсSave.setText("Сохранить");
        btnDecCoeffRecalсSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDecCoeffRecalсSaveActionPerformed(evt);
            }
        });
        pnlInProcess.add(btnDecCoeffRecalсSave);
        btnDecCoeffRecalсSave.setBounds(430, 150, 130, 30);

        btnSetDcRecalcPeriod.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnSetDcRecalcPeriod.setText("set");
        btnSetDcRecalcPeriod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetDcRecalcPeriodActionPerformed(evt);
            }
        });
        pnlInProcess.add(btnSetDcRecalcPeriod);
        btnSetDcRecalcPeriod.setBounds(500, 70, 60, 30);

        getContentPane().add(pnlInProcess);
        pnlInProcess.setBounds(10, 310, 570, 190);

        pnlCalibrationTable.setBorder(javax.swing.BorderFactory.createTitledBorder("Таблица калибровки"));
        pnlCalibrationTable.setLayout(null);

        lblTemperature.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTemperature.setText("<html><b><u>Температура</b></u></html>");
        pnlCalibrationTable.add(lblTemperature);
        lblTemperature.setBounds(10, 20, 270, 30);

        lblPhaseShift.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPhaseShift.setText("<html><b><u>Коэффициент вычета</b></u></html>");
        pnlCalibrationTable.add(lblPhaseShift);
        lblPhaseShift.setBounds(310, 20, 350, 30);

        btnT1Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT1Get.setText("req");
        btnT1Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT1GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT1Get);
        btnT1Get.setBounds(10, 50, 60, 30);

        edtT1Show.setEditable(false);
        edtT1Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtT1Show.setEnabled(false);
        pnlCalibrationTable.add(edtT1Show);
        edtT1Show.setBounds(80, 50, 60, 30);

        edtT1Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtT1Edit);
        edtT1Edit.setBounds(150, 50, 60, 30);

        btnT1Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT1Set.setText("set");
        btnT1Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT1SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT1Set);
        btnT1Set.setBounds(220, 50, 60, 30);

        btnPS1Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS1Get.setText("req");
        btnPS1Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS1GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS1Get);
        btnPS1Get.setBounds(310, 50, 60, 30);

        edtPS1Show.setEditable(false);
        edtPS1Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtPS1Show.setEnabled(false);
        pnlCalibrationTable.add(edtPS1Show);
        edtPS1Show.setBounds(380, 50, 100, 30);

        edtPS1Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtPS1Edit);
        edtPS1Edit.setBounds(490, 50, 100, 30);

        btnPS1Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS1Set.setText("set");
        btnPS1Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS1SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS1Set);
        btnPS1Set.setBounds(600, 50, 60, 30);

        btnT2Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT2Get.setText("req");
        btnT2Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT2GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT2Get);
        btnT2Get.setBounds(10, 90, 60, 30);

        edtT2Show.setEditable(false);
        edtT2Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtT2Show.setEnabled(false);
        pnlCalibrationTable.add(edtT2Show);
        edtT2Show.setBounds(80, 90, 60, 30);

        edtT2Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtT2Edit);
        edtT2Edit.setBounds(150, 90, 60, 30);

        btnT2Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT2Set.setText("set");
        btnT2Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT2SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT2Set);
        btnT2Set.setBounds(220, 90, 60, 30);

        btnPS2Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS2Get.setText("req");
        btnPS2Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS2GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS2Get);
        btnPS2Get.setBounds(310, 90, 60, 30);

        edtPS2Show.setEditable(false);
        edtPS2Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtPS2Show.setEnabled(false);
        pnlCalibrationTable.add(edtPS2Show);
        edtPS2Show.setBounds(380, 90, 100, 30);

        edtPS2Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtPS2Edit);
        edtPS2Edit.setBounds(490, 90, 100, 30);

        btnPS2Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS2Set.setText("set");
        btnPS2Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS2SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS2Set);
        btnPS2Set.setBounds(600, 90, 60, 30);

        btnT3Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT3Get.setText("req");
        btnT3Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT3GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT3Get);
        btnT3Get.setBounds(10, 130, 60, 30);

        edtT3Show.setEditable(false);
        edtT3Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtT3Show.setEnabled(false);
        pnlCalibrationTable.add(edtT3Show);
        edtT3Show.setBounds(80, 130, 60, 30);

        edtT3Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtT3Edit);
        edtT3Edit.setBounds(150, 130, 60, 30);

        btnT3Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT3Set.setText("set");
        btnT3Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT3SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT3Set);
        btnT3Set.setBounds(220, 130, 60, 30);

        btnPS3Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS3Get.setText("req");
        btnPS3Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS3GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS3Get);
        btnPS3Get.setBounds(310, 130, 60, 30);

        edtPS3Show.setEditable(false);
        edtPS3Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtPS3Show.setEnabled(false);
        pnlCalibrationTable.add(edtPS3Show);
        edtPS3Show.setBounds(380, 130, 100, 30);

        edtPS3Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtPS3Edit);
        edtPS3Edit.setBounds(490, 130, 100, 30);

        btnPS3Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS3Set.setText("set");
        btnPS3Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS3SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS3Set);
        btnPS3Set.setBounds(600, 130, 60, 30);

        btnT4Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT4Get.setText("req");
        btnT4Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT4GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT4Get);
        btnT4Get.setBounds(10, 170, 60, 30);

        edtT4Show.setEditable(false);
        edtT4Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtT4Show.setEnabled(false);
        pnlCalibrationTable.add(edtT4Show);
        edtT4Show.setBounds(80, 170, 60, 30);

        edtT4Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtT4Edit);
        edtT4Edit.setBounds(150, 170, 60, 30);

        btnT4Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT4Set.setText("set");
        btnT4Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT4SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT4Set);
        btnT4Set.setBounds(220, 170, 60, 30);

        btnT5Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT5Get.setText("req");
        btnT5Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT5GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT5Get);
        btnT5Get.setBounds(10, 210, 60, 30);

        edtT5Show.setEditable(false);
        edtT5Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtT5Show.setEnabled(false);
        edtT5Show.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                edtT5ShowActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(edtT5Show);
        edtT5Show.setBounds(80, 210, 60, 30);

        edtT5Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtT5Edit);
        edtT5Edit.setBounds(150, 210, 60, 30);

        btnT5Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT5Set.setText("set");
        btnT5Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT5SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT5Set);
        btnT5Set.setBounds(220, 210, 60, 30);

        btnPS4Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS4Get.setText("req");
        btnPS4Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS4GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS4Get);
        btnPS4Get.setBounds(310, 170, 60, 30);

        edtPS4Show.setEditable(false);
        edtPS4Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtPS4Show.setEnabled(false);
        pnlCalibrationTable.add(edtPS4Show);
        edtPS4Show.setBounds(380, 170, 100, 30);

        edtPS4Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtPS4Edit);
        edtPS4Edit.setBounds(490, 170, 100, 30);

        btnPS4Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS4Set.setText("set");
        btnPS4Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS4SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS4Set);
        btnPS4Set.setBounds(600, 170, 60, 30);

        btnPS5Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS5Get.setText("req");
        btnPS5Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS5GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS5Get);
        btnPS5Get.setBounds(310, 210, 60, 30);

        edtPS5Show.setEditable(false);
        edtPS5Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtPS5Show.setEnabled(false);
        pnlCalibrationTable.add(edtPS5Show);
        edtPS5Show.setBounds(380, 210, 100, 30);

        edtPS5Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtPS5Edit);
        edtPS5Edit.setBounds(490, 210, 100, 30);

        btnPS5Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS5Set.setText("set");
        btnPS5Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS5SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS5Set);
        btnPS5Set.setBounds(600, 210, 60, 30);

        btnT6Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT6Get.setText("req");
        btnT6Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT6GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT6Get);
        btnT6Get.setBounds(10, 250, 60, 30);

        edtT6Show.setEditable(false);
        edtT6Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtT6Show.setEnabled(false);
        pnlCalibrationTable.add(edtT6Show);
        edtT6Show.setBounds(80, 250, 60, 30);

        edtT6Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtT6Edit);
        edtT6Edit.setBounds(150, 250, 60, 30);

        btnT6Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT6Set.setText("set");
        btnT6Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT6SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT6Set);
        btnT6Set.setBounds(220, 250, 60, 30);

        btnT7Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT7Get.setText("req");
        btnT7Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT7GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT7Get);
        btnT7Get.setBounds(10, 290, 60, 30);

        edtT7Show.setEditable(false);
        edtT7Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtT7Show.setEnabled(false);
        pnlCalibrationTable.add(edtT7Show);
        edtT7Show.setBounds(80, 290, 60, 30);

        edtT7Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtT7Edit);
        edtT7Edit.setBounds(150, 290, 60, 30);

        btnT7Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT7Set.setText("set");
        btnT7Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT7SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT7Set);
        btnT7Set.setBounds(220, 290, 60, 30);

        btnT8Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT8Get.setText("req");
        btnT8Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT8GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT8Get);
        btnT8Get.setBounds(10, 330, 60, 30);

        edtT8Show.setEditable(false);
        edtT8Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtT8Show.setEnabled(false);
        pnlCalibrationTable.add(edtT8Show);
        edtT8Show.setBounds(80, 330, 60, 30);

        edtT8Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtT8Edit);
        edtT8Edit.setBounds(150, 330, 60, 30);

        btnT8Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT8Set.setText("set");
        btnT8Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT8SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT8Set);
        btnT8Set.setBounds(220, 330, 60, 30);

        btnT9Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT9Get.setText("req");
        btnT9Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT9GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT9Get);
        btnT9Get.setBounds(10, 370, 60, 30);

        edtT9Show.setEditable(false);
        edtT9Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtT9Show.setEnabled(false);
        pnlCalibrationTable.add(edtT9Show);
        edtT9Show.setBounds(80, 370, 60, 30);

        edtT9Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtT9Edit);
        edtT9Edit.setBounds(150, 370, 60, 30);

        btnT9Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT9Set.setText("set");
        btnT9Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT9SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT9Set);
        btnT9Set.setBounds(220, 370, 60, 30);

        btnT10Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT10Get.setText("req");
        btnT10Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT10GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT10Get);
        btnT10Get.setBounds(10, 410, 60, 30);

        edtT10Show.setEditable(false);
        edtT10Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtT10Show.setEnabled(false);
        pnlCalibrationTable.add(edtT10Show);
        edtT10Show.setBounds(80, 410, 60, 30);

        edtT10Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtT10Edit);
        edtT10Edit.setBounds(150, 410, 60, 30);

        btnT10Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT10Set.setText("set");
        btnT10Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT10SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT10Set);
        btnT10Set.setBounds(220, 410, 60, 30);

        btnT11Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT11Get.setText("req");
        btnT11Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT11GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT11Get);
        btnT11Get.setBounds(10, 450, 60, 30);

        edtT11Show.setEditable(false);
        edtT11Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtT11Show.setEnabled(false);
        pnlCalibrationTable.add(edtT11Show);
        edtT11Show.setBounds(80, 450, 60, 30);

        edtT11Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtT11Edit);
        edtT11Edit.setBounds(150, 450, 60, 30);

        btnT11Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnT11Set.setText("set");
        btnT11Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT11SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnT11Set);
        btnT11Set.setBounds(220, 450, 60, 30);

        btnPS6Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS6Get.setText("req");
        btnPS6Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS6GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS6Get);
        btnPS6Get.setBounds(310, 250, 60, 30);

        edtPS6Show.setEditable(false);
        edtPS6Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtPS6Show.setEnabled(false);
        pnlCalibrationTable.add(edtPS6Show);
        edtPS6Show.setBounds(380, 250, 100, 30);

        edtPS6Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtPS6Edit);
        edtPS6Edit.setBounds(490, 250, 100, 30);

        btnPS6Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS6Set.setText("set");
        btnPS6Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS6SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS6Set);
        btnPS6Set.setBounds(600, 250, 60, 30);

        btnPS7Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS7Get.setText("req");
        btnPS7Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS7GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS7Get);
        btnPS7Get.setBounds(310, 290, 60, 30);

        edtPS7Show.setEditable(false);
        edtPS7Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtPS7Show.setEnabled(false);
        pnlCalibrationTable.add(edtPS7Show);
        edtPS7Show.setBounds(380, 290, 100, 30);

        edtPS7Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtPS7Edit);
        edtPS7Edit.setBounds(490, 290, 100, 30);

        btnPS7Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS7Set.setText("set");
        btnPS7Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS7SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS7Set);
        btnPS7Set.setBounds(600, 290, 60, 30);

        btnPS8Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS8Get.setText("req");
        btnPS8Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS8GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS8Get);
        btnPS8Get.setBounds(310, 330, 60, 30);

        edtPS8Show.setEditable(false);
        edtPS8Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtPS8Show.setEnabled(false);
        pnlCalibrationTable.add(edtPS8Show);
        edtPS8Show.setBounds(380, 330, 100, 30);

        edtPS8Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtPS8Edit);
        edtPS8Edit.setBounds(490, 330, 100, 30);

        btnPS8Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS8Set.setText("set");
        btnPS8Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS8SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS8Set);
        btnPS8Set.setBounds(600, 330, 60, 30);

        btnPS9Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS9Get.setText("req");
        btnPS9Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS9GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS9Get);
        btnPS9Get.setBounds(310, 370, 60, 30);

        edtPS9Show.setEditable(false);
        edtPS9Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtPS9Show.setEnabled(false);
        pnlCalibrationTable.add(edtPS9Show);
        edtPS9Show.setBounds(380, 370, 100, 30);

        edtPS9Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtPS9Edit);
        edtPS9Edit.setBounds(490, 370, 100, 30);

        btnPS9Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS9Set.setText("set");
        btnPS9Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS9SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS9Set);
        btnPS9Set.setBounds(600, 370, 60, 30);

        btnPS10Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS10Get.setText("req");
        btnPS10Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS10GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS10Get);
        btnPS10Get.setBounds(310, 410, 60, 30);

        edtPS10Show.setEditable(false);
        edtPS10Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtPS10Show.setEnabled(false);
        pnlCalibrationTable.add(edtPS10Show);
        edtPS10Show.setBounds(380, 410, 100, 30);

        edtPS10Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtPS10Edit);
        edtPS10Edit.setBounds(490, 410, 100, 30);

        btnPS10Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS10Set.setText("set");
        btnPS10Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS10SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS10Set);
        btnPS10Set.setBounds(600, 410, 60, 30);

        btnPS11Get.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS11Get.setText("req");
        btnPS11Get.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS11GetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS11Get);
        btnPS11Get.setBounds(310, 450, 60, 30);

        edtPS11Show.setEditable(false);
        edtPS11Show.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtPS11Show.setEnabled(false);
        pnlCalibrationTable.add(edtPS11Show);
        edtPS11Show.setBounds(380, 450, 100, 30);

        edtPS11Edit.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pnlCalibrationTable.add(edtPS11Edit);
        edtPS11Edit.setBounds(490, 450, 100, 30);

        btnPS11Set.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        btnPS11Set.setText("set");
        btnPS11Set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPS11SetActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnPS11Set);
        btnPS11Set.setBounds(600, 450, 60, 30);

        btnResetCalibData.setText("Сбросить данные калибровки");
        btnResetCalibData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetCalibDataActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnResetCalibData);
        btnResetCalibData.setBounds(10, 600, 650, 30);

        btnSaveData.setText("Сохранить данные калибровки в память МК");
        btnSaveData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveDataActionPerformed(evt);
            }
        });
        pnlCalibrationTable.add(btnSaveData);
        btnSaveData.setBounds(10, 640, 650, 30);

        getContentPane().add(pnlCalibrationTable);
        pnlCalibrationTable.setBounds(590, 10, 670, 680);

        pnlCalcDc.setBorder(javax.swing.BorderFactory.createTitledBorder("Вычисление текущего Квычета"));
        pnlCalcDc.setLayout(null);

        lblCurrentoutputParam.setText("x");
        pnlCalcDc.add(lblCurrentoutputParam);
        lblCurrentoutputParam.setBounds(10, 20, 410, 30);

        btnSwitchCurrentOutputParam.setText("Переключить");
        btnSwitchCurrentOutputParam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSwitchCurrentOutputParamActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnSwitchCurrentOutputParam);
        btnSwitchCurrentOutputParam.setBounds(420, 20, 140, 30);

        btnCalcDcStart.setText("Старт");
        btnCalcDcStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCalcDcStartActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnCalcDcStart);
        btnCalcDcStart.setBounds(10, 60, 90, 30);

        SetCalcedDcAsStartValue.setText("Копировать в Квыч стартовый");
        SetCalcedDcAsStartValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetCalcedDcAsStartValueActionPerformed(evt);
            }
        });
        pnlCalcDc.add(SetCalcedDcAsStartValue);
        SetCalcedDcAsStartValue.setBounds(220, 60, 340, 30);

        btnCalcDcReset.setText("Сброс");
        btnCalcDcReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCalcDcResetActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnCalcDcReset);
        btnCalcDcReset.setBounds(110, 60, 90, 30);

        btnT1.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        btnT1.setText("1");
        btnT1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnT1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT1ActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnT1);
        btnT1.setBounds(220, 100, 40, 30);

        btnT2.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        btnT2.setText("2");
        btnT2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnT2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT2ActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnT2);
        btnT2.setBounds(250, 100, 40, 30);

        btnT3.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        btnT3.setText("3");
        btnT3.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnT3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT3ActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnT3);
        btnT3.setBounds(280, 100, 40, 30);

        btnT4.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        btnT4.setText("4");
        btnT4.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnT4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT4ActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnT4);
        btnT4.setBounds(310, 100, 40, 30);

        btnT5.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        btnT5.setText("5");
        btnT5.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnT5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT5ActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnT5);
        btnT5.setBounds(340, 100, 40, 30);

        btnT6.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        btnT6.setText("6");
        btnT6.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnT6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT6ActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnT6);
        btnT6.setBounds(370, 100, 40, 30);

        btnT7.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        btnT7.setText("7");
        btnT7.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnT7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT7ActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnT7);
        btnT7.setBounds(400, 100, 40, 30);

        btnT8.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        btnT8.setText("8");
        btnT8.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnT8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT8ActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnT8);
        btnT8.setBounds(430, 100, 40, 30);

        btnT9.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        btnT9.setText("9");
        btnT9.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnT9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT9ActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnT9);
        btnT9.setBounds(460, 100, 40, 30);

        btnT10.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        btnT10.setText("10");
        btnT10.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnT10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT10ActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnT10);
        btnT10.setBounds(490, 100, 40, 30);

        btnT11.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        btnT11.setText("11");
        btnT11.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnT11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnT11ActionPerformed(evt);
            }
        });
        pnlCalcDc.add(btnT11);
        btnT11.setBounds(520, 100, 40, 30);

        lblDcCalc_dN.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        lblDcCalc_dN.setText("dN");
        lblDcCalc_dN.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        pnlCalcDc.add(lblDcCalc_dN);
        lblDcCalc_dN.setBounds(80, 140, 70, 30);

        lblDcCalc_N.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        lblDcCalc_N.setText("N");
        lblDcCalc_N.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        pnlCalcDc.add(lblDcCalc_N);
        lblDcCalc_N.setBounds(10, 140, 70, 30);

        lblDcCalc_dU.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        lblDcCalc_dU.setText("dU");
        lblDcCalc_dU.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        pnlCalcDc.add(lblDcCalc_dU);
        lblDcCalc_dU.setBounds(260, 140, 70, 30);

        lblDcCalc_SdU.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        lblDcCalc_SdU.setText("SdU");
        lblDcCalc_SdU.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        pnlCalcDc.add(lblDcCalc_SdU);
        lblDcCalc_SdU.setBounds(330, 140, 110, 30);

        lblDcCalc_SdN.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        lblDcCalc_SdN.setText("SdN");
        lblDcCalc_SdN.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        pnlCalcDc.add(lblDcCalc_SdN);
        lblDcCalc_SdN.setBounds(150, 140, 110, 30);

        lblDcCalc_DC.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        lblDcCalc_DC.setText("DC");
        lblDcCalc_DC.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        pnlCalcDc.add(lblDcCalc_DC);
        lblDcCalc_DC.setBounds(440, 140, 120, 30);

        getContentPane().add(pnlCalcDc);
        pnlCalcDc.setBounds(10, 510, 570, 180);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConnectActionPerformed
        m_strPort = edtComPortValue.getText();
        if( m_strPort.isEmpty()) {
            logger.info( "Connect to no-port? Ha (3 times)");
            return;
        }
        
        theApp.m_bfCircleBuffer= new SLG_DCST_CircleBuffer();
        
        for( int i = 0; i < theApp.LIST_PARAMS_LEN; i++) {
            theApp.m_bParamTDefined[ i] = false;
            theApp.m_nParamDcDefined[ i] = 0;
        }
        
        serialPort = new SerialPort( m_strPort);
        try {
            //Открываем порт
            serialPort.openPort();

            //Выставляем параметры
            serialPort.setParams( 921600,
                                 SerialPort.DATABITS_8,
                                 SerialPort.STOPBITS_1,
                                 SerialPort.PARITY_NONE);

            //Включаем аппаратное управление потоком
            //serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | 
            //                              SerialPort.FLOWCONTROL_RTSCTS_OUT);

            //Устанавливаем ивент лисенер и маску
            m_evListener = new PortReader();
            serialPort.addEventListener( m_evListener, SerialPort.MASK_RXCHAR);
        }
        catch( SerialPortException ex) {
            logger.error( "COM-Communication exception", ex);
            theApp.m_bConnected = false;
            SLG_DCST_App.MessageBoxError( "При попытке соединения получили исключительную ситуацию:\n\n" + ex.toString(), "SLG_DCST");
            return;
        }
        
        theApp.m_strVersion = "";
        theApp.m_bConnected = true;
        theApp.m_nDecCoeffRecalc = SLG_DCST_App.DEC_COEFF_RECALC_UNKNOWN;
        theApp.m_bParamsChanged = false;
        theApp.m_nDecCoeffCurrent = 655350;
        theApp.m_nDecCoeffStart = 655350;
        theApp.m_dblTD1 = 0.;
        theApp.m_nDeviceRegime = SLG_Constants.SLG_REGIME_UNKNOWN;
        theApp.m_nMainParamOutput = SLG_Constants.SLG_MAIN_PARAM_OUTPUT_UNKNOWN;
    }//GEN-LAST:event_btnConnectActionPerformed

    private void btnResetCalibDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetCalibDataActionPerformed
        byte aBytes[] = new byte[4];
        aBytes[0] = SLG_ConstantsCmd.SLG_CMD_ACT_RESET_DC_CALIB;
        aBytes[1] = 0;
        aBytes[2] = 0;
        aBytes[3] = 0;
        
        try {
            serialPort.writeBytes( aBytes);
            theApp.m_bParamsChanged = true;
            logger.debug( ">> RESET DC CALIB");
            logger.debug( String.format( ">> 0x%02x 0x%02x 0x%02x 0x%02x", aBytes[0], aBytes[1], aBytes[2], aBytes[3]));
        } catch (SerialPortException ex) {
            logger.error( "COM-Communication exception", ex);
            theApp.m_bConnected = false;
            SLG_DCST_App.MessageBoxError( "При попытке записи в порт получили исключительную ситуацию:\n\n" + ex.toString(), "SLG_DCST");
        }
    }//GEN-LAST:event_btnResetCalibDataActionPerformed

    private void btnDecCoeffRecalcRecalcActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDecCoeffRecalcRecalcActionPerformed
        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_RECALC, ( byte) 0x00, ( byte) 0);
        theApp.m_bParamsChanged = true;
    }//GEN-LAST:event_btnDecCoeffRecalcRecalcActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        
        if( theApp.m_bParamsChanged == true && theApp.m_bConnected == true) {
            int nRespond = SLG_DCST_App.MessageBoxYesNo( "Параметры были изменены, но не сохранены в память микроконтроллера!\nВыйти без сохранения?", "SLG_DCST");
            if( nRespond == JOptionPane.NO_OPTION) return;
        }
        
        if( tRefreshStates != null) { tRefreshStates.stop(); tRefreshStates = null; }
        if( tRefreshValues != null) { tRefreshValues.stop(); tRefreshValues = null; }
        if( tPolling != null)       { tPolling.stop();       tPolling = null; }
        
        theApp.m_bConnected = false;
        try {
            if( serialPort != null && serialPort.isOpened()) {
                serialPort.removeEventListener();
                serialPort.closePort();
            }
            
            thrProcessorRunnable.m_bStopThread = true;
            thrProcessorThread.join( 1000);
            if( thrProcessorThread.isAlive()) {
                logger.error( "Thread stopped, but alive!");
            }
        }
        catch( SerialPortException ex) {
            logger.error( "COM-Communication exception", ex);
        } catch (InterruptedException ex) {
            logger.error( "Processing thread join fails", ex);
        }
        
        String strComPort = edtComPortValue.getText();
        if( !strComPort.isEmpty()) {
            theApp.GetSettings().SetComPort( strComPort);
            theApp.GetSettings().SaveSettings();
        }
    }//GEN-LAST:event_formWindowClosing

    private void btnDisconnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDisconnectActionPerformed

        if( theApp.m_bParamsChanged == true) {
            int nRespond = SLG_DCST_App.MessageBoxYesNo( "Параметры были изменены, но не сохранены в память микроконтроллера!\nОтсоединиться без сохранения?", "SLG_DCST");
            if( nRespond == JOptionPane.NO_OPTION) return;
        }
        
        theApp.m_bConnected = false;
        try {
            serialPort.removeEventListener();
            serialPort.closePort();
            
            /*
            thrProcessorRunnable.m_bStopThread = true;
            thrProcessorThread.join( 1000);
            if( thrProcessorThread.isAlive()) {
                logger.error( "Thread stopped, but alive!");
            }*/
        }
        catch( SerialPortException ex) {
            logger.error( "COM-Communication exception", ex);
        }
        /*
        catch (InterruptedException ex) {
            logger.error( "Processing thread join fails", ex);
        }
        */
    }//GEN-LAST:event_btnDisconnectActionPerformed

    public void SendComandRequestParam( byte btParam, byte btParamIndex) {
        byte aBytes[] = new byte[4];
        aBytes[0] = SLG_ConstantsCmd.SLG_CMD_REQ;
        aBytes[1] = btParam;
        aBytes[2] = btParamIndex;
        aBytes[3] = 0;
        
        try {
            serialPort.writeBytes( aBytes);
            logger.debug( ">> REQ PARAM_" + btParam + "." + btParamIndex);
            logger.debug( String.format( ">> 0x%02X 0x%02X 0x%02X 0x%02X", aBytes[0], aBytes[1], aBytes[2], aBytes[3]));
        } catch (SerialPortException ex) {
            logger.error( "COM-Communication exception", ex);
            theApp.m_bConnected = false;
            SLG_DCST_App.MessageBoxError( "При попытке записи в порт получили исключительную ситуацию:\n\n" + ex.toString(), "SLG_DCST");
        }
    }
    
    public void SendComandSetIndexedParam( byte btParam, byte btParamIndex, byte btParamValue) {
        byte aBytes[] = new byte[4];
        aBytes[0] = SLG_ConstantsCmd.SLG_CMD_SET;
        aBytes[1] = btParam;
        aBytes[2] = btParamIndex;
        aBytes[3] = btParamValue;
        
        try {
            serialPort.writeBytes( aBytes);
            logger.debug( ">> SET PARAM_" + btParam + "." + btParamIndex + "=" + btParamValue + String.format( "  (0x%02X)", btParamValue));
            logger.debug( String.format( ">> 0x%02X 0x%02X 0x%02X 0x%02X", aBytes[0], aBytes[1], aBytes[2], aBytes[3]));
        } catch (SerialPortException ex) {
            logger.error( "COM-Communication exception", ex);
            theApp.m_bConnected = false;
            SLG_DCST_App.MessageBoxError( "При попытке записи в порт получили исключительную ситуацию:\n\n" + ex.toString(), "SLG_DCST");
        }
    }
    
    public void SendComandSetParam( byte btParam, byte btValueL, byte btValueH) {
        byte aBytes[] = new byte[4];
        aBytes[0] = SLG_ConstantsCmd.SLG_CMD_SET;
        aBytes[1] = btParam;
        aBytes[2] = btValueL;
        aBytes[3] = btValueH;
        
        try {
            serialPort.writeBytes( aBytes);
            logger.debug( ">> SET PARAM_" + btParam + "=" + String.format( "0x%04X", btValueH * 256 + btValueL));
            logger.debug( String.format( ">> 0x%02X 0x%02X 0x%02X 0x%02X", aBytes[0], aBytes[1], aBytes[2], aBytes[3]));
        } catch (SerialPortException ex) {
            logger.error( "COM-Communication exception", ex);
            theApp.m_bConnected = false;
            SLG_DCST_App.MessageBoxError( "При попытке записи в порт получили исключительную ситуацию:\n\n" + ex.toString(), "SLG_DCST");
        }
    }
    
    private void btnT1GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT1GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 0);
    }//GEN-LAST:event_btnT1GetActionPerformed

    private void btnT2GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT2GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 1);
    }//GEN-LAST:event_btnT2GetActionPerformed

    private void btnT3GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT3GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 2);
    }//GEN-LAST:event_btnT3GetActionPerformed

    private void btnT4GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT4GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 3);
    }//GEN-LAST:event_btnT4GetActionPerformed

    private void btnT5GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT5GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 4);
    }//GEN-LAST:event_btnT5GetActionPerformed

    private void btnT6GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT6GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 5);
    }//GEN-LAST:event_btnT6GetActionPerformed

    private void btnT7GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT7GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 6);
    }//GEN-LAST:event_btnT7GetActionPerformed

    private void btnT8GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT8GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 7);
    }//GEN-LAST:event_btnT8GetActionPerformed

    private void btnT9GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT9GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 8);
    }//GEN-LAST:event_btnT9GetActionPerformed

    private void btnT10GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT10GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 9);
    }//GEN-LAST:event_btnT10GetActionPerformed

    private void btnT11GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT11GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 10);
    }//GEN-LAST:event_btnT11GetActionPerformed

    private void btnPS1GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS1GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 0);        
    }//GEN-LAST:event_btnPS1GetActionPerformed

    private void btnPS2GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS2GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 1);
    }//GEN-LAST:event_btnPS2GetActionPerformed

    private void btnPS3GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS3GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 2);
    }//GEN-LAST:event_btnPS3GetActionPerformed

    private void btnPS4GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS4GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 3);
    }//GEN-LAST:event_btnPS4GetActionPerformed

    private void btnPS5GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS5GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 4);
    }//GEN-LAST:event_btnPS5GetActionPerformed

    private void btnPS6GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS6GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 5);
    }//GEN-LAST:event_btnPS6GetActionPerformed

    private void btnPS7GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS7GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 6);
    }//GEN-LAST:event_btnPS7GetActionPerformed

    private void btnPS8GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS8GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 7);
    }//GEN-LAST:event_btnPS8GetActionPerformed

    private void btnPS9GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS9GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 8);
    }//GEN-LAST:event_btnPS9GetActionPerformed

    private void btnPS10GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS10GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 9);
    }//GEN-LAST:event_btnPS10GetActionPerformed

    private void btnPS11GetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS11GetActionPerformed
        SendComandRequestParam( ( byte )SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 10);
    }//GEN-LAST:event_btnPS11GetActionPerformed

    private void btnT1SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT1SetActionPerformed
        try {
            String strValue = edtT1Edit.getText();
            Integer IntValue = Integer.parseInt( strValue);
            if( IntValue >= -60 && IntValue <= +60) {
                IntValue += 128;
                byte btValue = IntValue.byteValue();
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 0, btValue);
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Температура должна быть в диапазоне [-60;+60]", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить температуру1\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnT1SetActionPerformed

    private void btnT2SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT2SetActionPerformed
        try {
            String strValue = edtT2Edit.getText();
            Integer IntValue = Integer.parseInt( strValue);
            if( IntValue >= -60 && IntValue <= +60) {
                IntValue += 128;
                byte btValue = IntValue.byteValue();
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 1, btValue);
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Температура должна быть в диапазоне [-60;+60]", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить температуру2\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnT2SetActionPerformed

    private void btnT3SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT3SetActionPerformed
        try {
            String strValue = edtT3Edit.getText();
            Integer IntValue = Integer.parseInt( strValue);
            if( IntValue >= -60 && IntValue <= +60) {
                IntValue += 128;
                byte btValue = IntValue.byteValue();
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 2, btValue);
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Температура должна быть в диапазоне [-60;+60]", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить температуру3\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnT3SetActionPerformed

    private void btnT4SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT4SetActionPerformed
        try {
            String strValue = edtT4Edit.getText();
            Integer IntValue = Integer.parseInt( strValue);
            if( IntValue >= -60 && IntValue <= +60) {
                IntValue += 128;
                byte btValue = IntValue.byteValue();
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 3, btValue);
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Температура должна быть в диапазоне [-60;+60]", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить температуру4\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnT4SetActionPerformed

    private void btnT5SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT5SetActionPerformed
        try {
            String strValue = edtT5Edit.getText();
            Integer IntValue = Integer.parseInt( strValue);
            if( IntValue >= -60 && IntValue <= +60) {
                IntValue += 128;
                byte btValue = IntValue.byteValue();
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 4, btValue);
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Температура должна быть в диапазоне [-60;+60]", "SLG_DCST");
            }

        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить температуру5\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnT5SetActionPerformed

    private void btnT6SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT6SetActionPerformed
        try {
            String strValue = edtT6Edit.getText();
            Integer IntValue = Integer.parseInt( strValue);
            if( IntValue >= -60 && IntValue <= +60) {
                IntValue += 128;
                byte btValue = IntValue.byteValue();
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 5, btValue);
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Температура должна быть в диапазоне [-60;+60]", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить температуру6\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnT6SetActionPerformed

    private void btnT7SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT7SetActionPerformed
        try {
            String strValue = edtT7Edit.getText();
            Integer IntValue = Integer.parseInt( strValue);
            if( IntValue >= -60 && IntValue <= +60) {
                IntValue += 128;
                byte btValue = IntValue.byteValue();
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 6, btValue);
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Температура должна быть в диапазоне [-60;+60]", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить температуру7\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnT7SetActionPerformed

    private void btnT8SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT8SetActionPerformed
        try {
            String strValue = edtT8Edit.getText();
            Integer IntValue = Integer.parseInt( strValue);
            if( IntValue >= -60 && IntValue <= +60) {
                IntValue += 128;
                byte btValue = IntValue.byteValue();
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 7, btValue);
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Температура должна быть в диапазоне [-60;+60]", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить температуру8\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnT8SetActionPerformed

    private void btnT9SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT9SetActionPerformed
        try {
            String strValue = edtT9Edit.getText();
            Integer IntValue = Integer.parseInt( strValue);
            if( IntValue >= -60 && IntValue <= +60) {
                IntValue += 128;
                byte btValue = IntValue.byteValue();
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 8, btValue);
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Температура должна быть в диапазоне [-60;+60]", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить температуру9\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnT9SetActionPerformed

    private void btnT10SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT10SetActionPerformed
        try {
            String strValue = edtT10Edit.getText();
            Integer IntValue = Integer.parseInt( strValue);
            if( IntValue >= -60 && IntValue <= +60) {
                IntValue += 128;
                byte btValue = IntValue.byteValue();
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 9, btValue);
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Температура должна быть в диапазоне [-60;+60]", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить температуру10\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnT10SetActionPerformed

    private void btnT11SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT11SetActionPerformed
        try {
            String strValue = edtT11Edit.getText();
            Integer IntValue = Integer.parseInt( strValue);
            if( IntValue >= -60 && IntValue <= +60) {
                IntValue += 128;
                byte btValue = IntValue.byteValue();
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_T, ( byte) 10, btValue);
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Температура должна быть в диапазоне [-60;+60]", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить температуру11\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnT11SetActionPerformed

    private void btnPS1SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS1SetActionPerformed
        try {
            String strValue = edtPS1Edit.getText();
            strValue = strValue.replace( ",", ".");
            Double DblValue = Double.parseDouble(strValue);
            if( DblValue >= 0. && DblValue < 1.) {
                
                short shValue = ( short) ( DblValue * 65535.);                
                byte btValueL = ( byte) ( shValue & 0xFF);                
                m_btValueH = ( byte) ((( shValue & 0xFF00) >> 8) & 0xFF);
                
                //logger.debug( "OOPS  sh="  + shValue +              String.format( "   (0x%02x)", shValue));
                //logger.debug( "OOPS  btL=" + ( btValueL & 0xFF) +   String.format( "   (0x%02x)", ( btValueL & 0xFF)));
                //logger.debug( "OOPS  btH=" + ( m_btValueH & 0xFF) + String.format( "   (0x%02x)", ( m_btValueH & 0xFF)));
                
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 0, btValueL);
                new Timer( 100, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (( Timer) e.getSource()).stop();
                        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H, ( byte) 0, m_btValueH);
                    }
                }).start();
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Значение коэффициента вычета должно быть в диапазоне [0;1)", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить Квыч1\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnPS1SetActionPerformed

    private void btnPS2SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS2SetActionPerformed
        try {
            String strValue = edtPS2Edit.getText();
            strValue = strValue.replace( ",", ".");
            Double DblValue = Double.parseDouble(strValue);
            if( DblValue >= 0. && DblValue < 1.) {
                
                short shValue = ( short) ( DblValue * 65535.);                
                byte btValueL = ( byte) ( shValue & 0xFF);                
                m_btValueH = ( byte) ((( shValue & 0xFF00) >> 8) & 0xFF);
                
                //logger.debug( "OOPS  sh="  + shValue +              String.format( "   (0x%02x)", shValue));
                //logger.debug( "OOPS  btL=" + ( btValueL & 0xFF) +   String.format( "   (0x%02x)", ( btValueL & 0xFF)));
                //logger.debug( "OOPS  btH=" + ( m_btValueH & 0xFF) + String.format( "   (0x%02x)", ( m_btValueH & 0xFF)));
                
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 1, btValueL);
                new Timer( 100, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (( Timer) e.getSource()).stop();
                        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H, ( byte) 1, m_btValueH);
                    }
                }).start();
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Значение коэффициента вычета должно быть в диапазоне [0;1)", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить Квыч2\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnPS2SetActionPerformed

    private void btnPS3SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS3SetActionPerformed
        try {
            String strValue = edtPS3Edit.getText();
            strValue = strValue.replace( ",", ".");
            Double DblValue = Double.parseDouble(strValue);
            if( DblValue >= 0. && DblValue < 1.) {
                
                short shValue = ( short) ( DblValue * 65535.);                
                byte btValueL = ( byte) ( shValue & 0xFF);                
                m_btValueH = ( byte) ((( shValue & 0xFF00) >> 8) & 0xFF);
                
                //logger.debug( "OOPS  sh="  + shValue +              String.format( "   (0x%02x)", shValue));
                //logger.debug( "OOPS  btL=" + ( btValueL & 0xFF) +   String.format( "   (0x%02x)", ( btValueL & 0xFF)));
                //logger.debug( "OOPS  btH=" + ( m_btValueH & 0xFF) + String.format( "   (0x%02x)", ( m_btValueH & 0xFF)));
                
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 2, btValueL);
                new Timer( 100, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (( Timer) e.getSource()).stop();
                        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H, ( byte) 2, m_btValueH);
                    }
                }).start();
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Значение коэффициента вычета должно быть в диапазоне [0;1)", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить Квыч3\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnPS3SetActionPerformed

    private void btnPS4SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS4SetActionPerformed
        try {
            String strValue = edtPS4Edit.getText();
            strValue = strValue.replace( ",", ".");
            Double DblValue = Double.parseDouble(strValue);
            if( DblValue >= 0. && DblValue < 1.) {
                
                short shValue = ( short) ( DblValue * 65535.);                
                byte btValueL = ( byte) ( shValue & 0xFF);                
                m_btValueH = ( byte) ((( shValue & 0xFF00) >> 8) & 0xFF);
                
                //logger.debug( "OOPS  sh="  + shValue +              String.format( "   (0x%02x)", shValue));
                //logger.debug( "OOPS  btL=" + ( btValueL & 0xFF) +   String.format( "   (0x%02x)", ( btValueL & 0xFF)));
                //logger.debug( "OOPS  btH=" + ( m_btValueH & 0xFF) + String.format( "   (0x%02x)", ( m_btValueH & 0xFF)));
                
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 3, btValueL);
                new Timer( 100, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (( Timer) e.getSource()).stop();
                        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H, ( byte) 3, m_btValueH);
                    }
                }).start();
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Значение коэффициента вычета должно быть в диапазоне [0;1)", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить Квыч4\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnPS4SetActionPerformed

    private void btnPS5SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS5SetActionPerformed
        try {
            String strValue = edtPS5Edit.getText();
            strValue = strValue.replace( ",", ".");
            Double DblValue = Double.parseDouble(strValue);
            if( DblValue >= 0. && DblValue < 1.) {
                
                short shValue = ( short) ( DblValue * 65535.);                
                byte btValueL = ( byte) ( shValue & 0xFF);                
                m_btValueH = ( byte) ((( shValue & 0xFF00) >> 8) & 0xFF);
                
                //logger.debug( "OOPS  sh="  + shValue +              String.format( "   (0x%02x)", shValue));
                //logger.debug( "OOPS  btL=" + ( btValueL & 0xFF) +   String.format( "   (0x%02x)", ( btValueL & 0xFF)));
                //logger.debug( "OOPS  btH=" + ( m_btValueH & 0xFF) + String.format( "   (0x%02x)", ( m_btValueH & 0xFF)));
                
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 4, btValueL);
                new Timer( 100, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (( Timer) e.getSource()).stop();
                        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H, ( byte) 4, m_btValueH);
                    }
                }).start();
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Значение коэффициента вычета должно быть в диапазоне [0;1)", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить Квыч5\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnPS5SetActionPerformed

    private void btnPS6SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS6SetActionPerformed
        try {
            String strValue = edtPS6Edit.getText();
            strValue = strValue.replace( ",", ".");
            Double DblValue = Double.parseDouble(strValue);
            if( DblValue >= 0. && DblValue < 1.) {
                
                short shValue = ( short) ( DblValue * 65535.);                
                byte btValueL = ( byte) ( shValue & 0xFF);                
                m_btValueH = ( byte) ((( shValue & 0xFF00) >> 8) & 0xFF);
                
                //logger.debug( "OOPS  sh="  + shValue +              String.format( "   (0x%02x)", shValue));
                //logger.debug( "OOPS  btL=" + ( btValueL & 0xFF) +   String.format( "   (0x%02x)", ( btValueL & 0xFF)));
                //logger.debug( "OOPS  btH=" + ( m_btValueH & 0xFF) + String.format( "   (0x%02x)", ( m_btValueH & 0xFF)));
                
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 5, btValueL);
                new Timer( 100, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (( Timer) e.getSource()).stop();
                        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H, ( byte) 5, m_btValueH);
                    }
                }).start();
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Значение коэффициента вычета должно быть в диапазоне [0;1)", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить Квыч6\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnPS6SetActionPerformed

    private void btnPS7SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS7SetActionPerformed
        try {
            String strValue = edtPS7Edit.getText();
            strValue = strValue.replace( ",", ".");
            Double DblValue = Double.parseDouble(strValue);
            if( DblValue >= 0. && DblValue < 1.) {
                
                short shValue = ( short) ( DblValue * 65535.);                
                byte btValueL = ( byte) ( shValue & 0xFF);                
                m_btValueH = ( byte) ((( shValue & 0xFF00) >> 8) & 0xFF);
                
                //logger.debug( "OOPS  sh="  + shValue +              String.format( "   (0x%02x)", shValue));
                //logger.debug( "OOPS  btL=" + ( btValueL & 0xFF) +   String.format( "   (0x%02x)", ( btValueL & 0xFF)));
                //logger.debug( "OOPS  btH=" + ( m_btValueH & 0xFF) + String.format( "   (0x%02x)", ( m_btValueH & 0xFF)));
                
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 6, btValueL);
                new Timer( 100, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (( Timer) e.getSource()).stop();
                        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H, ( byte) 6, m_btValueH);
                    }
                }).start();
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Значение коэффициента вычета должно быть в диапазоне [0;1)", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить Квыч7\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnPS7SetActionPerformed

    private void btnPS8SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS8SetActionPerformed
        try {
            String strValue = edtPS8Edit.getText();
            strValue = strValue.replace( ",", ".");
            Double DblValue = Double.parseDouble(strValue);
            if( DblValue >= 0. && DblValue < 1.) {
                
                short shValue = ( short) ( DblValue * 65535.);                
                byte btValueL = ( byte) ( shValue & 0xFF);                
                m_btValueH = ( byte) ((( shValue & 0xFF00) >> 8) & 0xFF);
                
                //logger.debug( "OOPS  sh="  + shValue +              String.format( "   (0x%02x)", shValue));
                //logger.debug( "OOPS  btL=" + ( btValueL & 0xFF) +   String.format( "   (0x%02x)", ( btValueL & 0xFF)));
                //logger.debug( "OOPS  btH=" + ( m_btValueH & 0xFF) + String.format( "   (0x%02x)", ( m_btValueH & 0xFF)));
                
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 7, btValueL);
                new Timer( 100, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (( Timer) e.getSource()).stop();
                        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H, ( byte) 7, m_btValueH);
                    }
                }).start();
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Значение коэффициента вычета должно быть в диапазоне [0;1)", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить Квыч8\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnPS8SetActionPerformed

    private void btnPS9SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS9SetActionPerformed
        try {
            String strValue = edtPS9Edit.getText();
            strValue = strValue.replace( ",", ".");
            Double DblValue = Double.parseDouble(strValue);
            if( DblValue >= 0. && DblValue < 1.) {
                
                short shValue = ( short) ( DblValue * 65535.);                
                byte btValueL = ( byte) ( shValue & 0xFF);                
                m_btValueH = ( byte) ((( shValue & 0xFF00) >> 8) & 0xFF);
                
                //logger.debug( "OOPS  sh="  + shValue +              String.format( "   (0x%02x)", shValue));
                //logger.debug( "OOPS  btL=" + ( btValueL & 0xFF) +   String.format( "   (0x%02x)", ( btValueL & 0xFF)));
                //logger.debug( "OOPS  btH=" + ( m_btValueH & 0xFF) + String.format( "   (0x%02x)", ( m_btValueH & 0xFF)));
                
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 8, btValueL);
                new Timer( 100, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (( Timer) e.getSource()).stop();
                        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H, ( byte) 8, m_btValueH);
                    }
                }).start();
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Значение коэффициента вычета должно быть в диапазоне [0;1)", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить Квыч9\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnPS9SetActionPerformed

    private void btnPS10SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS10SetActionPerformed
        try {
            String strValue = edtPS10Edit.getText();
            strValue = strValue.replace( ",", ".");
            Double DblValue = Double.parseDouble(strValue);
            if( DblValue >= 0. && DblValue < 1.) {
                
                short shValue = ( short) ( DblValue * 65535.);                
                byte btValueL = ( byte) ( shValue & 0xFF);                
                m_btValueH = ( byte) ((( shValue & 0xFF00) >> 8) & 0xFF);
                
                //logger.debug( "OOPS  sh="  + shValue +              String.format( "   (0x%02x)", shValue));
                //logger.debug( "OOPS  btL=" + ( btValueL & 0xFF) +   String.format( "   (0x%02x)", ( btValueL & 0xFF)));
                //logger.debug( "OOPS  btH=" + ( m_btValueH & 0xFF) + String.format( "   (0x%02x)", ( m_btValueH & 0xFF)));
                
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 9, btValueL);
                new Timer( 100, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (( Timer) e.getSource()).stop();
                        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H, ( byte) 9, m_btValueH);
                    }
                }).start();
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Значение коэффициента вычета должно быть в диапазоне [0;1)", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить Квыч10\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnPS10SetActionPerformed

    private void btnPS11SetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPS11SetActionPerformed
        try {
            String strValue = edtPS11Edit.getText();
            strValue = strValue.replace( ",", ".");
            Double DblValue = Double.parseDouble(strValue);
            if( DblValue >= 0. && DblValue < 1.) {
                
                short shValue = ( short) ( DblValue * 65535.);                
                byte btValueL = ( byte) ( shValue & 0xFF);                
                m_btValueH = ( byte) ((( shValue & 0xFF00) >> 8) & 0xFF);
                
                //logger.debug( "OOPS  sh="  + shValue +              String.format( "   (0x%02x)", shValue));
                //logger.debug( "OOPS  btL=" + ( btValueL & 0xFF) +   String.format( "   (0x%02x)", ( btValueL & 0xFF)));
                //logger.debug( "OOPS  btH=" + ( m_btValueH & 0xFF) + String.format( "   (0x%02x)", ( m_btValueH & 0xFF)));
                
                SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_L, ( byte) 10, btValueL);
                new Timer( 100, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        (( Timer) e.getSource()).stop();
                        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_CALIB_DC_H, ( byte) 10, m_btValueH);
                    }
                }).start();
                theApp.m_bParamsChanged = true;
            }
            else {
                SLG_DCST_App.MessageBoxInfo( "Значение коэффициента вычета должно быть в диапазоне [0;1)", "SLG_DCST");
            }
        }
        catch( NumberFormatException e) {
            logger.error( "При обработке команды \"отправить Квыч11\" возникла исключительная ситуация!", e);
        }
    }//GEN-LAST:event_btnPS11SetActionPerformed

    private void btnSaveDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveDataActionPerformed
        byte aBytes[] = new byte[4];
        aBytes[0] = SLG_ConstantsCmd.SLG_CMD_ACT_SAVE_FLASH_PARAM;
        aBytes[1] = 3;
        aBytes[2] = 0;
        aBytes[3] = 0;
        
        try {
            serialPort.writeBytes( aBytes);
            theApp.m_bParamsChanged = false;
            logger.debug( ">> SAVE PH_SH CALIB");
            logger.debug( String.format( ">> 0x%02x 0x%02x 0x%02x 0x%02x", aBytes[0], aBytes[1], aBytes[2], aBytes[3]));
        } catch (SerialPortException ex) {
            logger.error( "COM-Communication exception", ex);
            theApp.m_bConnected = false;
            SLG_DCST_App.MessageBoxError( "При попытке записи в порт получили исключительную ситуацию:\n\n" + ex.toString(), "SLG_DCST");
        }
    }//GEN-LAST:event_btnSaveDataActionPerformed

    private void btnDecCoeffRecalcCalibApproxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDecCoeffRecalcCalibApproxActionPerformed
        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_RECALC, ( byte) 0x02, ( byte) 0);
        theApp.m_bParamsChanged = true;
    }//GEN-LAST:event_btnDecCoeffRecalcCalibApproxActionPerformed

    private void btnDecCoeffRecalcManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDecCoeffRecalcManualActionPerformed
        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_RECALC, ( byte) 0x03, ( byte) 0);
        theApp.m_bParamsChanged = true;
    }//GEN-LAST:event_btnDecCoeffRecalcManualActionPerformed

    private void btnDecCoeffRecalcCalibHardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDecCoeffRecalcCalibHardActionPerformed
        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_RECALC, ( byte) 0x01, ( byte) 0);
        theApp.m_bParamsChanged = true;
    }//GEN-LAST:event_btnDecCoeffRecalcCalibHardActionPerformed

    private void btnDcStartSetTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDcStartSetTableActionPerformed
        logger.error( "FUCK!");
        logger.error( "FUCK!");
        logger.error( "FUCK!");
        logger.error( "FUCK! CHECK IF TABLE CORRECT!");
        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_START_DEF, ( byte) 0x01, ( byte) 0);
        theApp.m_bParamsChanged = true;
    }//GEN-LAST:event_btnDcStartSetTableActionPerformed

    private void btnSaveDcStartDefAndValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveDcStartDefAndValueActionPerformed
        byte aBytes[] = new byte[4];
        aBytes[0] = SLG_ConstantsCmd.SLG_CMD_ACT_SAVE_FLASH_PARAM;
        aBytes[1] = 0;
        aBytes[2] = 0;
        aBytes[3] = 0;
        
        try {            
            logger.debug( ">> SAVE P1");
            logger.debug( String.format( ">> 0x%02x 0x%02x 0x%02x 0x%02x", aBytes[0], aBytes[1], aBytes[2], aBytes[3]));
            serialPort.writeBytes( aBytes);
            theApp.m_bParamsChanged = false;
        } catch (SerialPortException ex) {
            logger.error( "COM-Communication exception", ex);
            theApp.m_bConnected = false;
            SLG_DCST_App.MessageBoxError( "При попытке записи в порт получили исключительную ситуацию:\n\n" + ex.toString(), "SLG_DCST");
        }
        
        new Timer( 100, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                (( Timer) e.getSource()).stop();
                
                byte aBytes[] = new byte[4];
                aBytes[0] = SLG_ConstantsCmd.SLG_CMD_ACT_SAVE_FLASH_PARAM;
                aBytes[1] = 3;
                aBytes[2] = 0;
                aBytes[3] = 0;
        
                try {            
                    logger.debug( ">> SAVE P4");
                    logger.debug( String.format( ">> 0x%02x 0x%02x 0x%02x 0x%02x", aBytes[0], aBytes[1], aBytes[2], aBytes[3]));
                    serialPort.writeBytes( aBytes);
                    theApp.m_bParamsChanged = false;
                } catch( SerialPortException ex) {
                    logger.error( "COM-Communication exception", ex);
                    theApp.m_bConnected = false;
                    SLG_DCST_App.MessageBoxError( "При попытке записи в порт получили исключительную ситуацию:\n\n" + ex.toString(), "SLG_DCST");
                }
            }
        }).start();
        
        theApp.m_bParamsChanged = true;
    }//GEN-LAST:event_btnSaveDcStartDefAndValueActionPerformed

    private void btnSetDcStartValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetDcStartValueActionPerformed
        String strStartDcValue = edtDcStartSetValue.getText();
        strStartDcValue = strStartDcValue.replace( ",", ".");
        double dblStartDcValue;
        try {
            dblStartDcValue = Double.parseDouble( strStartDcValue);
        } catch( NumberFormatException ex) {
            SLG_DCST_App.MessageBoxError( "Неверно задан стартовый коэффициент вычета!", "SLG_DCST");
            return;
        }
        int nStartDcValue = ( int) ( dblStartDcValue * 655350.);
        
        byte b1 = ( byte) ( nStartDcValue & 0xFF);
        byte b2 = ( byte) ( ( nStartDcValue & 0xFF00) >> 8);
        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_START, b1, b2);
        
        theApp.m_bParamsChanged = true;
    }//GEN-LAST:event_btnSetDcStartValueActionPerformed

    private void btnDcStartSetDcStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDcStartSetDcStartActionPerformed
        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_START_DEF, ( byte) 0x00, ( byte) 0);
        theApp.m_bParamsChanged = true;
    }//GEN-LAST:event_btnDcStartSetDcStartActionPerformed

    private void btnDecCoeffRecalсSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDecCoeffRecalсSaveActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnDecCoeffRecalсSaveActionPerformed

    private void btnSwitchCurrentOutputParamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSwitchCurrentOutputParamActionPerformed
        byte aBytes[] = new byte[4];
        aBytes[0] = SLG_ConstantsCmd.SLG_CMD_ACT_SWC_DW_DNDU_OUTPUT;
        if( theApp.m_nMainParamOutput == SLG_Constants.SLG_MAIN_PARAM_OUTPUT_DNDU)
            aBytes[1] = 0;
        if( theApp.m_nMainParamOutput == SLG_Constants.SLG_MAIN_PARAM_OUTPUT_DPHI)
            aBytes[1] = 1;
        
        aBytes[2] = 0;
        aBytes[3] = 0;
        
        try {            
            logger.debug( ">> SWITCH MAIN PARAM OUTPUT");
            logger.debug( String.format( ">> 0x%02X 0x%02X 0x%02X 0x%02X", aBytes[0], aBytes[1], aBytes[2], aBytes[3]));
            serialPort.writeBytes( aBytes);
        } catch (SerialPortException ex) {
            logger.error( "COM-Communication exception", ex);
            theApp.m_bConnected = false;
            SLG_DCST_App.MessageBoxError( "При попытке записи в порт получили исключительную ситуацию:\n\n" + ex.toString(), "SLG_DCST");
        }
    }//GEN-LAST:event_btnSwitchCurrentOutputParamActionPerformed

    private void edtT5ShowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_edtT5ShowActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_edtT5ShowActionPerformed

    private void btnSetDcRecalcPeriodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetDcRecalcPeriodActionPerformed
        String strDcRecalcPeriod = edtDcRecalcPeriodSet.getText();
        int nDcRecalcPeriod = 600;
        try {
            nDcRecalcPeriod = Integer.parseInt( strDcRecalcPeriod);
        }
        catch( NumberFormatException ex) {
            SLG_DCST_App.MessageBoxError( "Плохой период переопределения Квычета!", "SLG_DCST");
            return;
        }
        
        byte b1 = ( byte) ( nDcRecalcPeriod & 0xFF);
        byte b2 = ( byte) ( ( nDcRecalcPeriod & 0xFF00) >> 8);
        SendComandSetParam( ( byte ) SLG_ConstantsParams.SLG_PARAM_DC_RECALC_PERIOD, ( byte) b1, ( byte) b2);
        theApp.m_bParamsChanged = true;
    }//GEN-LAST:event_btnSetDcRecalcPeriodActionPerformed

    private void SetCalcedDcAsStartValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SetCalcedDcAsStartValueActionPerformed
        String dc = String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU);
        edtDcStartSetValue.setText( dc);
    }//GEN-LAST:event_SetCalcedDcAsStartValueActionPerformed

    private void btnCalcDcStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCalcDcStartActionPerformed
        if( theApp.m_bDcCalculation) {
            theApp.m_bDcCalculation = false;
            btnCalcDcStart.setText( "Старт");
        }
        else {
            theApp.m_bDcCalculation = true;
            btnCalcDcStart.setText( "Стоп");
        }
    }//GEN-LAST:event_btnCalcDcStartActionPerformed

    private void btnCalcDcResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCalcDcResetActionPerformed
        theApp.m_lSumm_dN = 0;
        theApp.m_lSumm_dU = 0;
        theApp.m_lDcCalcCounter = 0;
    }//GEN-LAST:event_btnCalcDcResetActionPerformed

    private void btnT1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT1ActionPerformed
        edtPS1Edit.setText( String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
    }//GEN-LAST:event_btnT1ActionPerformed

    private void btnT2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT2ActionPerformed
        edtPS2Edit.setText( String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
    }//GEN-LAST:event_btnT2ActionPerformed

    private void btnT3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT3ActionPerformed
        edtPS3Edit.setText( String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
    }//GEN-LAST:event_btnT3ActionPerformed

    private void btnT4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT4ActionPerformed
        edtPS4Edit.setText( String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
    }//GEN-LAST:event_btnT4ActionPerformed

    private void btnT5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT5ActionPerformed
        edtPS5Edit.setText( String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
    }//GEN-LAST:event_btnT5ActionPerformed

    private void btnT6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT6ActionPerformed
        edtPS6Edit.setText( String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
    }//GEN-LAST:event_btnT6ActionPerformed

    private void btnT7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT7ActionPerformed
        edtPS7Edit.setText( String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
    }//GEN-LAST:event_btnT7ActionPerformed

    private void btnT8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT8ActionPerformed
        edtPS8Edit.setText( String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
    }//GEN-LAST:event_btnT8ActionPerformed

    private void btnT9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT9ActionPerformed
        edtPS9Edit.setText( String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
    }//GEN-LAST:event_btnT9ActionPerformed

    private void btnT10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT10ActionPerformed
        edtPS10Edit.setText( String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
    }//GEN-LAST:event_btnT10ActionPerformed

    private void btnT11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnT11ActionPerformed
        edtPS11Edit.setText( String.format( "%.06f", ( double) theApp.m_lSumm_dN / ( double) theApp.m_lSumm_dU));
    }//GEN-LAST:event_btnT11ActionPerformed


    private class PortReader implements SerialPortEventListener {

        @Override
        public void serialEvent(SerialPortEvent event) {            
            if( event.isRXCHAR() && event.getEventValue() > 0){
                try {
                    //Получаем ответ от устройства, обрабатываем данные и т.д.
                    int nReadyBytes = event.getEventValue();
                    byte bts[] = new byte[ nReadyBytes];
                    bts = serialPort.readBytes( nReadyBytes);
                    
                    /*
                    String strLogMessage;
                    strLogMessage = String.format( "READ %d BYTE. FIRST ONE=0x%02X", nReadyBytes, bts[0]);
                    logger.debug( strLogMessage);
                    */
                    
                    theApp.m_bfCircleBuffer.AddBytes( bts, nReadyBytes);
                }
                catch (SerialPortException ex) {
                    logger.error( "SerialPortException caught", ex);
                }
            }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton SetCalcedDcAsStartValue;
    private javax.swing.JButton btnCalcDcReset;
    private javax.swing.JButton btnCalcDcStart;
    public javax.swing.JButton btnConnect;
    private javax.swing.JButton btnDcStartSetDcStart;
    private javax.swing.JButton btnDcStartSetTable;
    private javax.swing.JButton btnDecCoeffRecalcCalibApprox;
    private javax.swing.JButton btnDecCoeffRecalcCalibHard;
    private javax.swing.JButton btnDecCoeffRecalcManual;
    private javax.swing.JButton btnDecCoeffRecalcRecalc;
    private javax.swing.JButton btnDecCoeffRecalсSave;
    public javax.swing.JButton btnDisconnect;
    public javax.swing.JButton btnPS10Get;
    public javax.swing.JButton btnPS10Set;
    public javax.swing.JButton btnPS11Get;
    public javax.swing.JButton btnPS11Set;
    public javax.swing.JButton btnPS1Get;
    public javax.swing.JButton btnPS1Set;
    public javax.swing.JButton btnPS2Get;
    public javax.swing.JButton btnPS2Set;
    public javax.swing.JButton btnPS3Get;
    public javax.swing.JButton btnPS3Set;
    public javax.swing.JButton btnPS4Get;
    public javax.swing.JButton btnPS4Set;
    public javax.swing.JButton btnPS5Get;
    public javax.swing.JButton btnPS5Set;
    public javax.swing.JButton btnPS6Get;
    public javax.swing.JButton btnPS6Set;
    public javax.swing.JButton btnPS7Get;
    public javax.swing.JButton btnPS7Set;
    public javax.swing.JButton btnPS8Get;
    public javax.swing.JButton btnPS8Set;
    public javax.swing.JButton btnPS9Get;
    public javax.swing.JButton btnPS9Set;
    private javax.swing.JButton btnResetCalibData;
    private javax.swing.JButton btnSaveData;
    private javax.swing.JButton btnSaveDcStartDefAndValue;
    public javax.swing.JButton btnSetDcRecalcPeriod;
    public javax.swing.JButton btnSetDcStartValue;
    private javax.swing.JButton btnSwitchCurrentOutputParam;
    private javax.swing.JButton btnT1;
    private javax.swing.JButton btnT10;
    public javax.swing.JButton btnT10Get;
    public javax.swing.JButton btnT10Set;
    private javax.swing.JButton btnT11;
    public javax.swing.JButton btnT11Get;
    public javax.swing.JButton btnT11Set;
    public javax.swing.JButton btnT1Get;
    public javax.swing.JButton btnT1Set;
    private javax.swing.JButton btnT2;
    public javax.swing.JButton btnT2Get;
    public javax.swing.JButton btnT2Set;
    private javax.swing.JButton btnT3;
    public javax.swing.JButton btnT3Get;
    public javax.swing.JButton btnT3Set;
    private javax.swing.JButton btnT4;
    public javax.swing.JButton btnT4Get;
    public javax.swing.JButton btnT4Set;
    private javax.swing.JButton btnT5;
    public javax.swing.JButton btnT5Get;
    public javax.swing.JButton btnT5Set;
    private javax.swing.JButton btnT6;
    public javax.swing.JButton btnT6Get;
    public javax.swing.JButton btnT6Set;
    private javax.swing.JButton btnT7;
    public javax.swing.JButton btnT7Get;
    public javax.swing.JButton btnT7Set;
    private javax.swing.JButton btnT8;
    public javax.swing.JButton btnT8Get;
    public javax.swing.JButton btnT8Set;
    private javax.swing.JButton btnT9;
    public javax.swing.JButton btnT9Get;
    public javax.swing.JButton btnT9Set;
    private javax.swing.JTextField edtComPortValue;
    private javax.swing.JTextField edtDcRecalcPeriodCurrent;
    private javax.swing.JTextField edtDcRecalcPeriodSet;
    private javax.swing.JTextField edtDcStartCurrentValue;
    private javax.swing.JTextField edtDcStartSetValue;
    private javax.swing.JTextField edtPS10Edit;
    private javax.swing.JTextField edtPS10Show;
    private javax.swing.JTextField edtPS11Edit;
    private javax.swing.JTextField edtPS11Show;
    private javax.swing.JTextField edtPS1Edit;
    private javax.swing.JTextField edtPS1Show;
    private javax.swing.JTextField edtPS2Edit;
    private javax.swing.JTextField edtPS2Show;
    private javax.swing.JTextField edtPS3Edit;
    private javax.swing.JTextField edtPS3Show;
    private javax.swing.JTextField edtPS4Edit;
    private javax.swing.JTextField edtPS4Show;
    private javax.swing.JTextField edtPS5Edit;
    private javax.swing.JTextField edtPS5Show;
    private javax.swing.JTextField edtPS6Edit;
    private javax.swing.JTextField edtPS6Show;
    private javax.swing.JTextField edtPS7Edit;
    private javax.swing.JTextField edtPS7Show;
    private javax.swing.JTextField edtPS8Edit;
    private javax.swing.JTextField edtPS8Show;
    private javax.swing.JTextField edtPS9Edit;
    private javax.swing.JTextField edtPS9Show;
    private javax.swing.JTextField edtT10Edit;
    private javax.swing.JTextField edtT10Show;
    private javax.swing.JTextField edtT11Edit;
    private javax.swing.JTextField edtT11Show;
    private javax.swing.JTextField edtT1Edit;
    private javax.swing.JTextField edtT1Show;
    private javax.swing.JTextField edtT2Edit;
    private javax.swing.JTextField edtT2Show;
    private javax.swing.JTextField edtT3Edit;
    private javax.swing.JTextField edtT3Show;
    private javax.swing.JTextField edtT4Edit;
    private javax.swing.JTextField edtT4Show;
    private javax.swing.JTextField edtT5Edit;
    private javax.swing.JTextField edtT5Show;
    private javax.swing.JTextField edtT6Edit;
    private javax.swing.JTextField edtT6Show;
    private javax.swing.JTextField edtT7Edit;
    private javax.swing.JTextField edtT7Show;
    private javax.swing.JTextField edtT8Edit;
    private javax.swing.JTextField edtT8Show;
    private javax.swing.JTextField edtT9Edit;
    private javax.swing.JTextField edtT9Show;
    private javax.swing.JLabel lblConnectionStateTitle;
    private javax.swing.JLabel lblConnectionStateValue;
    private javax.swing.JLabel lblCurrentDecCoeffTitle;
    private javax.swing.JLabel lblCurrentDecCoeffValue;
    private javax.swing.JLabel lblCurrentTD1Title;
    private javax.swing.JLabel lblCurrentTD1Value;
    private javax.swing.JLabel lblCurrentoutputParam;
    private javax.swing.JLabel lblDcCalc_DC;
    private javax.swing.JLabel lblDcCalc_N;
    private javax.swing.JLabel lblDcCalc_SdN;
    private javax.swing.JLabel lblDcCalc_SdU;
    private javax.swing.JLabel lblDcCalc_dN;
    private javax.swing.JLabel lblDcCalc_dU;
    private javax.swing.JLabel lblDcStartDcStart;
    private javax.swing.JLabel lblDcStartTable;
    private javax.swing.JLabel lblDecCoeffRecalcPeriodValuePrefix;
    private javax.swing.JLabel lblDecCoeffRecalcTitle;
    private javax.swing.JLabel lblDecCoeffRecalcUnits;
    private javax.swing.JLabel lblPhaseShift;
    private javax.swing.JLabel lblPort;
    private javax.swing.JLabel lblSignDecCoeffRecalcCalibApprox;
    private javax.swing.JLabel lblSignDecCoeffRecalcCalibHard;
    private javax.swing.JLabel lblSignDecCoeffRecalcManual;
    private javax.swing.JLabel lblSignDecCoeffRecalcRecalc;
    private javax.swing.JLabel lblTemperature;
    private javax.swing.JPanel pnlCalcDc;
    private javax.swing.JPanel pnlCalibrationTable;
    private javax.swing.JPanel pnlCurrentParams;
    private javax.swing.JPanel pnlInProcess;
    private javax.swing.JPanel pnlStartParameters;
    // End of variables declaration//GEN-END:variables
}
