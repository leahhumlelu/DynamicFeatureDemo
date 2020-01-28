package com.hcljapan.dynamicfeaturedemo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.play.core.splitinstall.SplitInstallException;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends BaseSplitActivity implements View.OnClickListener, SplitInstallStateUpdatedListener {
    private static final String TAG = "MainActivity";
    private String moduleInstall,moduleDemand, moduleKungfu, moduleJudo;
    //private List<String> installableModules;
    private SplitInstallManager manager;
    public static final String PACKAGE_NAME = "com.hcljapan.dynamicfeature";
    public static final String PACKAGE_NAME_AT_INSTALL = PACKAGE_NAME+"install"+".AtInstallActivity";
    public static final String PACKAGE_NAME_ON_DEMAND = PACKAGE_NAME+"ondemand"+".OnDemandActivity";
    public static final String PACKAGE_NAME_KUNGFU= PACKAGE_NAME+"kungfu"+".KungfuActivity";
    public static final String PACKAGE_NAME_JUDO = PACKAGE_NAME+"judo"+".JudoActivity";

    private int sessionId =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        moduleInstall = getString(R.string.module_feature_at_install);
        moduleKungfu = getString(R.string.module_feature_kungfu);
        moduleDemand = getString(R.string.module_feature_on_demand);
        moduleJudo =getString(R.string.module_feature_judo);
        //installableModules = Arrays.asList(moduleInstall,moduleDemand,moduleKungfu);
        manager = SplitInstallManagerFactory.create(this);
        findViewById(R.id.btn_load_feature_at_install).setOnClickListener(this);
        findViewById(R.id.btn_load_feature_on_demand).setOnClickListener(this);
        findViewById(R.id.btn_load_feature_when_in_india).setOnClickListener(this);
        findViewById(R.id.btn_load_feature_when_in_china).setOnClickListener(this);
        findViewById(R.id.btn_uninstall_feature_at_install).setOnClickListener(this);
        findViewById(R.id.btn_uninstall_feature_on_demand).setOnClickListener(this);
        findViewById(R.id.btn_uninstall_feature_conditional).setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        manager.registerListener(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        manager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_load_feature_at_install:
                loadAndLaunchModule(moduleInstall);
                break;
            case R.id.btn_load_feature_on_demand:
                loadAndLaunchModule(moduleDemand);
                break;
            case R.id.btn_load_feature_when_in_india:
                loadAndLaunchModule(moduleJudo);
                break;
            case R.id.btn_uninstall_feature_at_install:
                uninstallModule(moduleInstall);
                break;
            case R.id.btn_uninstall_feature_on_demand:
                uninstallModule(moduleDemand);
                break;
            case R.id.btn_uninstall_feature_conditional:
                uninstallModule(moduleKungfu);
                break;
            case R.id.btn_load_feature_when_in_china:
                loadAndLaunchModule(moduleKungfu);
                break;
        }
    }

    private void loadAndLaunchModule(final String moduleName) {
        if(manager.getInstalledModules().contains(moduleName)){
            toastAndLog("loadAndLaunchModule: "+moduleName+ " already installed");
            onSuccessfulLoad(moduleName,true);
            return;
        }

        SplitInstallRequest request = SplitInstallRequest.newBuilder().addModule(moduleName).build();
        manager.startInstall(request).addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                sessionId = result;
                toastAndLog("install "+moduleName+" successful sessionid "+result);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                switch (((SplitInstallException)e).getErrorCode()){
                    case SplitInstallErrorCode
                            .NETWORK_ERROR:
                        toastAndLog("NETWORK error install " + moduleName + " exception " + e.getLocalizedMessage());
                        break;
                    case SplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED:
                        checkForActiveDownloads();
                        break;
                    case SplitInstallErrorCode.MODULE_UNAVAILABLE:
                        toastAndLog("module "+moduleName+" not available exception "+e.getLocalizedMessage());
                        break;
                }
            }
        });
        toastAndLog("loadAndLaunchModule: start installation for module "+moduleName);
    }

    private void checkForActiveDownloads() {
        manager.getSessionStates().addOnCompleteListener(new OnCompleteListener<List<SplitInstallSessionState>>() {
            @Override
            public void onComplete(Task<List<SplitInstallSessionState>> task) {
                if(task.isSuccessful()){
                    for(SplitInstallSessionState state:task.getResult()){
                        if(state.status()== SplitInstallSessionStatus.DOWNLOADING){
                            manager.cancelInstall(state.sessionId());
                        }
                    }
                }

            }
        });
    }


    private void onSuccessfulLoad(String moduleName, boolean launch) {
        if(launch){
            if(moduleName.equals(moduleInstall)){
                launchActivity(PACKAGE_NAME_AT_INSTALL);
            }else if(moduleName.equals(moduleDemand)){
                launchActivity(PACKAGE_NAME_ON_DEMAND);
            }else if(moduleName.equals(moduleKungfu)){
                launchActivity(PACKAGE_NAME_KUNGFU);
            }else if(moduleName.equals(moduleJudo)){
                launchActivity(PACKAGE_NAME_JUDO);
            }
        }
    }

    private void launchActivity(String packageNameToLaunch) {
        Intent intent = new Intent();
        intent.setClassName(BuildConfig.APPLICATION_ID,packageNameToLaunch);
        startActivity(intent);
    }

    @Override
    public void onStateUpdate(SplitInstallSessionState state) {
        if(state.status()==SplitInstallSessionStatus.FAILED && state.errorCode()==SplitInstallErrorCode.SERVICE_DIED){
            //TODO RETRY THE REQUEST
            return;
        }
        //todo
       if(state.sessionId()==sessionId){
           toastAndLog("loading");
           switch (state.status()){
               case SplitInstallSessionStatus.DOWNLOADING:
                   long totalBytes = state.totalBytesToDownload();
                   long progress = state.bytesDownloaded();
                   float ratio = progress / totalBytes * 100;
                   toastAndLog(progress + "/"+totalBytes+" has been downloaded");
                   break;
               case SplitInstallSessionStatus.INSTALLED:
                   //todo
                   break;

           }
       }
    }

    private void uninstallModule(final String moduleName){
        List<String> uninstallModuleList = Arrays.asList(moduleName);

        if(manager.getInstalledModules().contains(moduleName)){
            manager.deferredUninstall(uninstallModuleList).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    toastAndLog("uninstall "+moduleName+ " successful");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    toastAndLog("uninstall "+moduleName+ " fail");
                }
            });
        }

    }

    private void toastAndLog(String text){
        Toast.makeText(this,text,Toast.LENGTH_SHORT).show();
        Log.d(TAG, "toastAndLog: "+text);
    }
}
