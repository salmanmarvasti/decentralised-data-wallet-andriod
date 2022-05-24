package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.lifi.Chain;
import com.alphawallet.app.entity.lifi.Connection;
import com.alphawallet.app.entity.lifi.Quote;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.SwapService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Hex;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class SwapViewModel extends BaseViewModel
{
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final SwapService swapService;
    private final MutableLiveData<List<Chain>> chains = new MutableLiveData<>();
    private final MutableLiveData<Chain> chain = new MutableLiveData<>();
    private final MutableLiveData<List<Connection>> connections = new MutableLiveData<>();
    private final MutableLiveData<Quote> quote = new MutableLiveData<>();
    private final MutableLiveData<Long> network = new MutableLiveData<>();
    private Disposable chainsDisposable;
    private Disposable connectionsDisposable;
    private Disposable quoteDisposable;

    @Inject
    public SwapViewModel(
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            SwapService swapService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.swapService = swapService;
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public LiveData<List<Chain>> chains()
    {
        return chains;
    }

    public LiveData<Chain> chain()
    {
        return chain;
    }

    public LiveData<List<Connection>> connections()
    {
        return connections;
    }

    public LiveData<Quote> quote()
    {
        return quote;
    }


    public LiveData<Long> network()
    {
        return network;
    }

    public void setChain(Chain c)
    {
        chain.postValue(c);
    }

    public Chain getChain()
    {
        return chain.getValue();
    }

    public void getChains()
    {
        progress.postValue(true);

        chainsDisposable = swapService.getChains()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onChains, this::onError);
    }

    public void getConnections(long from, long to)
    {
        progress.postValue(true);

        connectionsDisposable = swapService.getConnections(from, to)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnections, this::onError);
    }

    public void getQuote(Connection.LToken source, Connection.LToken dest, String address, String amount, String slippage)
    {
        progress.postValue(true);

        quoteDisposable = swapService.getQuote(source, dest, address, amount, slippage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onQuote, this::onError);
    }

    private void onChains(String result)
    {
        try
        {
            JSONObject obj = new JSONObject(result);
            if (obj.has("chains"))
            {
                JSONArray chainz = obj.getJSONArray("chains");

                List<Chain> chainList = new Gson().fromJson(chainz.toString(), new TypeToken<List<Chain>>()
                {
                }.getType());

                chains.postValue(chainList);
            }
            else
            {
                error.postValue(new ErrorEnvelope(result));
            }
        }
        catch (JSONException e)
        {
            error.postValue(new ErrorEnvelope(e.getMessage()));
        }
    }

    private void onConnections(String result)
    {
        try
        {
            JSONObject obj = new JSONObject(result);
            if (obj.has("connections"))
            {
                JSONArray connz = obj.getJSONArray("connections");

                List<Connection> connectionz = new Gson().fromJson(connz.toString(), new TypeToken<List<Connection>>()
                {
                }.getType());

                connections.postValue(connectionz);
            }
            else
            {
                error.postValue(new ErrorEnvelope(result));
            }
        }
        catch (JSONException e)
        {
            error.postValue(new ErrorEnvelope(e.getMessage()));
        }

        progress.postValue(false);
    }

    private void onQuote(String result)
    {
        if (!isValidQuote(result))
        {
            error.postValue(new ErrorEnvelope(result));
        }
        else
        {
            Quote q = new Gson().fromJson(result, Quote.class);
            quote.postValue(q);
        }

        progress.postValue(false);
    }

    private boolean isValidQuote(String result)
    {
        return result.contains("id")
                && result.contains("action")
                && result.contains("tool");
    }

    public String getBalance(String walletAddress, Connection.LToken token)
    {
        String address = token.address;
        if (address.equalsIgnoreCase("0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")) // TODO:
        {
            address = walletAddress;
        }
        Token t = tokensService.getToken(token.chainId, address);
        if (t != null) return BalanceUtils.getShortFormat(t.balance.toString(), t.tokenInfo.decimals);
        else return "";
    }

    public void sendTransaction(Quote quote)
    {
        // TODO: send transaction
        buildWeb3Transaction(quote);
    }

    private Web3Transaction buildWeb3Transaction(Quote quote)
    {
        Quote.TransactionRequest request = quote.transactionRequest;

        return new Web3Transaction(
                new Address(request.from),
                new Address(request.to),
                Hex.hexToBigInteger(request.value, BigInteger.ZERO),
                Hex.hexToBigInteger(request.gasPrice, BigInteger.ZERO),
                Hex.hexToBigInteger(request.gasLimit, BigInteger.ZERO),
                -1,
                request.data
        );
    }


}