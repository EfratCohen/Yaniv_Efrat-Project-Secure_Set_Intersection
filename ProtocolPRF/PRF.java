package ProtocolPRF;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

import edu.biu.scapi.primitives.dlog.DlogGroup;
import edu.biu.scapi.primitives.dlog.GroupElement;
import edu.biu.scapi.primitives.dlog.miracl.MiraclDlogECFp;

public class PRF
{
	private int m;
	private BigInteger[] keys;
	private GroupElement g ;
	private BigInteger q;
	private DlogGroup dlog;
	
	public PRF(int m_param) throws IOException
	{
		m = m_param;
		keys = new  BigInteger[m+1];
		dlog = new MiraclDlogECFp();
		g = dlog.getGenerator();
		q = dlog.getOrder();
	}
	
	public void generateKey()
	{
		for(int i=0; i<=m;i++){
	     keys[i]= randomBigInteger(q);   
		}
		
	}
	public BigInteger getKeyi(int i)
	{
	   return keys[i];
	}
	public int getm()
	{
	   return m;
	}
	public  BigInteger randomBigInteger(BigInteger maxNum)
	{
	    Random rnd = new Random();
	    int maxNumBitLength = maxNum.bitLength();
	    BigInteger rndNum;
	    do {
	        rndNum = new BigInteger(maxNumBitLength, rnd);
	    } while(rndNum.compareTo(maxNum) >= 0);
	    return rndNum;
	}
	
	public GroupElement computeBlock(int x)
	{
		GroupElement fk=null;
		BigInteger mult=keys[0];
		for(int i=1;i<=m;i++)
		{
			if(x%2 == 1)
				mult= mult.multiply(keys[i]).mod(dlog.getOrder());
			x = x/2;
		}
		fk = dlog.exponentiate(g, mult);
		return fk;
	}
}