package my.sub.model;

/**
 * 业务异常
 */
public class BizException extends RuntimeException{
    public BizException(String message) {
        super(message);
    }
    public BizException(String message, Throwable cause) {
        super(message, cause);
    }
}
