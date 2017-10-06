/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flavt.slg.dec.coeff.setup.tool.communication;

import static java.lang.Math.min;
import org.apache.log4j.Logger;

/**
 *
 * @author yaroslav
 */
public class SLG_DCST_CircleBuffer {
    
    static Logger logger = Logger.getLogger(SLG_DCST_CircleBuffer.class);
    
    public static final int BUFFER_OK = 0;
    public static final int BUFFER_EXCEEDED = 1;
    public static final int BUFFER_NO_CR = 2;
            
    private int m_nStatus;
    
    public static final int BUFFER_LEN = 4096;
    private final int MAX_RESP_LEN = 500;
            
    private byte m_btBuffer[];
    
    private volatile int m_nLeftPos;
    private volatile int m_nRightPos;
    
    private volatile boolean m_bOverRound;
    private volatile boolean m_bFirstRound;
            
    
    public SLG_DCST_CircleBuffer() {
        m_btBuffer = new byte[ BUFFER_LEN];
        for( int i = 0; i < BUFFER_LEN; m_btBuffer[ i++] = 0);
        
        m_nStatus = BUFFER_OK;
        
        m_nLeftPos = 0;
        m_nRightPos = 0;
        
        m_bOverRound = false;
        m_bFirstRound = true;
        
        //logger.setLevel( AMSApp.LOG_LEVEL);
        //logger.setLevel( Level.OFF);
    }
    
    /**
     * Print circle buffer state: left_marker, right_marker, bytes
     * 
     */
    public void PrintState() {
        String strOut =  "PrintState(): L: " + m_nLeftPos + "  ";
               strOut += "R: " + m_nRightPos + "  ";
               strOut += "FR: " + m_bFirstRound + "  ";
               strOut += "OR: " + m_bOverRound;

        logger.debug( strOut);
        
        int l = ( BUFFER_LEN + m_nLeftPos - 5) % BUFFER_LEN;
        int r = ( BUFFER_LEN + m_nRightPos + 5) % BUFFER_LEN;
        
        strOut = "[";
        
        int indx = l;
        do {
            strOut += "" + m_btBuffer[ indx] + ", ";
            indx = ( indx++) % BUFFER_LEN;
            logger.trace( indx);
        } while( indx == r);
        
        strOut += "]";
        logger.debug( strOut);
    }
    
    /**
     * Get circle buffer validation status
     * 
     * @return 
     * 0 = BUFFER_OK
     * 1 = BUFFER_EXCEEDED
     * rest - look in CircleBuffer constants at the top
     * 
     */
    public int getStatus() { return m_nStatus; }
    
    /**
     * Add bytes to buffer
     * @param bytes
     * bytes to add
     * @return 
     * returns validation status of the object (here we can overround circle buffer)
     */
    public int AddBytes( byte[] bytes, int len) {
        if( m_nStatus != BUFFER_OK) return m_nStatus;
        
        //copy bytes
        for( int i=0; i < len; m_btBuffer[ (m_nRightPos++) % BUFFER_LEN] = bytes[i++]);
        
        //check overround
        if( m_nRightPos >= BUFFER_LEN) {
            m_nRightPos = m_nRightPos % BUFFER_LEN;
            
            m_bOverRound = true;
            m_bFirstRound = false;
        }
        
        //check exceeds
        if( m_bFirstRound) {
            if( m_nRightPos <= m_nLeftPos) {
                logger.fatal( "AddBytes(): На первом кругу правая позиция меньше левой (L,R): " + m_nLeftPos + " " + m_nRightPos);
                m_nStatus = BUFFER_EXCEEDED;
            }
        }
        else {
            if( m_bOverRound) {
                if( m_nRightPos >= m_nLeftPos) {
                    logger.fatal( "AddBytes(): С переходом круга, правая позиция больше левой (L,R): " + m_nLeftPos + " " + m_nRightPos);
                    m_nStatus = BUFFER_EXCEEDED;
                }
            }
            else {
                if( m_nLeftPos >= m_nRightPos) {
                    logger.fatal( "AddBytes(): Без перехода круга, левая позиция больше правой (L,R): " + m_nLeftPos + " " + m_nRightPos);
                    m_nStatus = BUFFER_EXCEEDED;
                }
            }        
        }
        
        logger.trace( "AddBytes(): result: l:" + m_nLeftPos + "  r:" + m_nRightPos + "  FR:" + m_bFirstRound + "  OR:" + m_bOverRound);
        return m_nStatus;
    }
    
    /**
     * Checks if (CR) is present within MAX_RESP_LEN
     * @return 
     * 0 - problems, or nothing
     * rest - length of the answer
     */
    public synchronized int isAnswerReady() {
        if( m_nStatus != BUFFER_OK) return 0;
        
        int len = min( MAX_RESP_LEN, ( BUFFER_LEN + m_nRightPos - m_nLeftPos) % BUFFER_LEN);
        int i;
        for( i = 0; i < len; i++) {
            if( m_btBuffer[ ( i + m_nLeftPos) % BUFFER_LEN] == -1) {
                return (i + 1);
            }
        }
        
        if( len == MAX_RESP_LEN)
            m_nStatus = BUFFER_NO_CR;
        
        return 0;
    }
    
    /**
     * Gets ready data length
     * @return 
     * returns ready data len
     */
    public int getReadyIncomingDataLen() {
        int len = ( BUFFER_LEN + m_nRightPos - m_nLeftPos) % BUFFER_LEN;
        logger.trace( "getReadyIncomingDataLen(): L=" + m_nLeftPos + " R=" + m_nRightPos + " len=" + len);
        return len;
    }
    
    /**
     * Gets len length answer from buffer
     * @param len requested data length (in bytes)
     * @param bts user buffer for receiving bytes
     * @return
     * 0 - ok<br>
     * 1 - CircleBuffer instance is not in valid state<br>
     * 2 - user asks for more data bytes than we have in our buffer
     */
    public synchronized int getAnswer( int len, byte [] bts) {
        if( m_nStatus != BUFFER_OK) return 1;
        
        //check if user asks for more data than we have
        if( len > getReadyIncomingDataLen())
            return 2; 
        
        for( int i=0; i<len; i++) {
            bts[i] = m_btBuffer[ m_nLeftPos];
            m_nLeftPos++;
            if( m_nLeftPos >= BUFFER_LEN) {
                m_nLeftPos = 0;
                m_bOverRound = false;
            }
        }
        
        return 0;
    }
    
    /*
    public static void main(String args[]) {
        BasicConfigurator.configure();
        CircleBuffer bf = new CircleBuffer();
        
        byte [] bts = new byte [5];
        for( int i = 0; i < 5; bts[ i++] = 0);
        bts[0] = '>';
        bts[1] = '3';
        bts[2] = 'E';
        bts[3] = '\r';
        for( int i = 0; i < 1000; i++) {
            logger.debug( "\n*******");
            bf.AddBytes( bts, 4);
            
            int len = bf.isAnswerReady();
            logger.debug( "isAnswerReady(): "+ len);
            
            if( len > 0) {
                String strResponse = bf.getAnswer( len);
                logger.debug( "Response: " + strResponse);
            }
            
            logger.debug( "getStatus(): "+ bf.getStatus());
        }
    }
    */
}
