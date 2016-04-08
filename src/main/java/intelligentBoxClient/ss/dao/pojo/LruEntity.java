package intelligentBoxClient.ss.dao.pojo;

/**
 * Created by Leo on 4/7/16.
 */
public class LruEntity {

    private String _curr;
    private String _prev;
    private String _next;

    public String getCurr(){ return _curr;}
    public void setCurr(String curr){ _curr = curr;}

    public String getPrev(){ return _prev;}
    public void setPrev(String prev){ _prev = prev;}

    public String getNext(){ return _next;}
    public void setNext(String next){ _next = next;}
}
