// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from addresses.djinni

package co.ledger.core;

import java.util.ArrayList;

public final class EthereumLikeNetworkParameters {


    /*package*/ final String Identifier;

    /*package*/ final String MessagePrefix;

    /*package*/ final byte[] XPUBVersion;

    /*package*/ final ArrayList<String> AdditionalEIPs;

    /*package*/ final long TimestampDelay;

    public EthereumLikeNetworkParameters(
            String Identifier,
            String MessagePrefix,
            byte[] XPUBVersion,
            ArrayList<String> AdditionalEIPs,
            long TimestampDelay) {
        this.Identifier = Identifier;
        this.MessagePrefix = MessagePrefix;
        this.XPUBVersion = XPUBVersion;
        this.AdditionalEIPs = AdditionalEIPs;
        this.TimestampDelay = TimestampDelay;
    }

    public String getIdentifier() {
        return Identifier;
    }

    public String getMessagePrefix() {
        return MessagePrefix;
    }

    public byte[] getXPUBVersion() {
        return XPUBVersion;
    }

    public ArrayList<String> getAdditionalEIPs() {
        return AdditionalEIPs;
    }

    public long getTimestampDelay() {
        return TimestampDelay;
    }

    @Override
    public String toString() {
        return "EthereumLikeNetworkParameters{" +
                "Identifier=" + Identifier +
                "," + "MessagePrefix=" + MessagePrefix +
                "," + "XPUBVersion=" + XPUBVersion +
                "," + "AdditionalEIPs=" + AdditionalEIPs +
                "," + "TimestampDelay=" + TimestampDelay +
        "}";
    }

}