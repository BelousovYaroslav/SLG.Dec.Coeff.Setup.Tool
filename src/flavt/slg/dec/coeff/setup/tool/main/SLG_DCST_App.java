/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flavt.slg.dec.coeff.setup.tool.main;

import flavt.slg.dec.coeff.setup.tool.communication.SLG_DCST_CircleBuffer;
import flavt.slg.lib.constants.SLG_Constants;
import java.io.File;
import java.net.ServerSocket;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author yaroslav
 */
public class SLG_DCST_App {
    public SLG_DCST_MainFrame m_pMainWnd;
    
    private ServerSocket m_pSingleInstanceSocketServer;
    
    public boolean m_bConnected;
    private final String m_strSLGrootEnvVar;
    public String GetSLGRoot() { return m_strSLGrootEnvVar; }
    
    static Logger logger = Logger.getLogger(SLG_DCST_App.class);

    private final SLG_DCST_Settings m_pSettings;
    public SLG_DCST_Settings GetSettings() { return m_pSettings; }
    
    public SLG_DCST_CircleBuffer m_bfCircleBuffer;
    
    public final int LIST_PARAMS_LEN = 11;
    
    public boolean m_bParamTDefined[];
    public int m_DevT[];
    public int m_nParamDcDefined[];
    public int m_nDevDc[];
    
    public String m_strVersion;
    public int m_nMainParamOutput;
    
    public static final int DEC_COEFF_STARTDEF_DCSTART = 0;
    public static final int DEC_COEFF_STARTDEF_CALIB = 1;
    public static final int DEC_COEFF_STARTDEF_UNKNOWN = 2;
    
    public static final int DEC_COEFF_RECALC_RECALC = 0;
    public static final int DEC_COEFF_RECALC_CALIB_HARD = 1;
    public static final int DEC_COEFF_RECALC_CALIB_APPROX = 2;
    public static final int DEC_COEFF_RECALC_MANUAL = 3;
    public static final int DEC_COEFF_RECALC_UNKNOWN = 0xFF;
    
    public int m_nDeviceRegime = SLG_Constants.SLG_REGIME_UNKNOWN;
    
    public int m_nDecCoeffCurrent;
    public int m_nDecCoeffStart;
    public int m_nDecCoeffStartDef;
    public int m_nDecCoeffRecalc;
    public int m_nDecCoeffRecalcPeriod;
    
    public double m_dblTD1;
    
    public boolean m_bParamsChanged;
    
    public int m_nMarkerFails;
    public int m_nCounterFails;
    public int m_nCheckSummFails;
    public int m_nPacksCounter;
    
    public SLG_DCST_App() {
        m_strSLGrootEnvVar = System.getenv( "SLG_ROOT");
        
        m_bParamTDefined = new boolean[ LIST_PARAMS_LEN];
        m_DevT = new int[ LIST_PARAMS_LEN];
        m_nParamDcDefined = new int[ LIST_PARAMS_LEN];
        m_nDevDc = new int[ LIST_PARAMS_LEN];
        
        for( int i = 0; i < LIST_PARAMS_LEN; i++) {
            m_bParamTDefined[i] = false; m_nParamDcDefined[i] = 0;
            m_DevT[i] = 0xFFFF; m_nDevDc[i] = 0xFFFF;
        }
        
        //SETTINGS
        m_pSettings = new SLG_DCST_Settings( m_strSLGrootEnvVar);
        
        m_pSingleInstanceSocketServer = null;
        //ПРОВЕРКА ОДНОВРЕМЕННОГО ЗАПУСКА ТОЛЬКО ОДНОЙ КОПИИ ПРОГРАММЫ
        try {
            m_pSingleInstanceSocketServer = new ServerSocket( m_pSettings.GetSingleInstanceSocketServerPort());
        }
        catch( Exception ex) {
            MessageBoxError( "Уже есть запущенный экземпляр утилиты редактирования параметров калибровки коэффициента вычета.\nПоищите на других \"экранах\".", "Утилита редактирования калибровки коэффициента вычета");
            logger.error( "Не смогли открыть сокет для проверки запуска только одной копии программы! Программа уже запущена?", ex);
            m_pSingleInstanceSocketServer = null;
            return;
        }
        
        m_bConnected = false;
        m_strVersion = "";
        m_nDecCoeffRecalc = DEC_COEFF_RECALC_UNKNOWN;
        m_nDecCoeffRecalcPeriod = 65535;
        m_nDecCoeffStartDef = SLG_DCST_App.DEC_COEFF_STARTDEF_UNKNOWN;
        m_nDecCoeffStart = 65535;
        m_nDecCoeffCurrent = 65535;
        m_dblTD1 = 0.;
        m_nDeviceRegime = SLG_Constants.SLG_REGIME_UNKNOWN;
        m_nMainParamOutput = SLG_Constants.SLG_MAIN_PARAM_OUTPUT_UNKNOWN;
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        //главная переменная окружения
        String strSLGrootEnvVar = System.getenv( "SLG_ROOT");
        if( strSLGrootEnvVar == null) {
            MessageBoxError( "Не задана переменная окружения SLG_ROOT!", "SLG_DCST");
            return;
        }
        
        //настройка логгера
        String strlog4jPropertiesFile = strSLGrootEnvVar + "/etc/log4j.dec.coeff.setup.tool.properties";
        File file = new File( strlog4jPropertiesFile);
        if(!file.exists())
            System.out.println("It is not possible to load the given log4j properties file :" + file.getAbsolutePath());
        else {
            String strAbsPath = file.getAbsolutePath();
            PropertyConfigurator.configure( strAbsPath);
        }
        
        SLG_DCST_App appInstance = new SLG_DCST_App();
        if( appInstance.m_pSingleInstanceSocketServer != null) {
            logger.info( "SLG_DCST_APP::main(): Start point!");
            appInstance.start();
        }
    }
    
    public void start() {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger( SLG_DCST_MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger( SLG_DCST_MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger( SLG_DCST_MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger( SLG_DCST_MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        
        m_pMainWnd = new SLG_DCST_MainFrame( this);
        java.awt.EventQueue.invokeLater( new Runnable() {
            public void run() {
                m_pMainWnd.setVisible( true);
            }
        });
    }
    
    /**
     * Функция для сообщения пользователю информационного сообщения
     * @param strMessage сообщение
     * @param strTitleBar заголовок
     */
    public static void MessageBoxInfo( String strMessage, String strTitleBar)
    {
        JOptionPane.showMessageDialog( null, strMessage, strTitleBar, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Функция для сообщения пользователю сообщения об ошибке
     * @param strMessage сообщение
     * @param strTitleBar заголовок
     */
    public static void MessageBoxError( String strMessage, String strTitleBar)
    {
        JOptionPane.showMessageDialog( null, strMessage, strTitleBar, JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Функция для опроса пользователя с ответом ДА - НЕТ
     * @param strMessage сообщение
     * @param strTitleBar заголовок
     * @return JOptionPane.YES_OPTION<br>или<br>
     * JOptionPane.NO_OPTION
     */
    public static int MessageBoxYesNo( String strMessage, String strTitleBar)
    {
        UIManager.put("OptionPane.noButtonText", "Нет");
        UIManager.put("OptionPane.okButtonText", "Согласен");
        UIManager.put("OptionPane.yesButtonText", "Да");
        
        int nReply = JOptionPane.showConfirmDialog( null, strMessage, strTitleBar, JOptionPane.YES_NO_OPTION);
        return nReply;
    }
}
