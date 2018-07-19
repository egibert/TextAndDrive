package fr.neamar.kiss.result;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.DataHolder;
import fr.neamar.kiss.utils.SpaceTokenizer;

public class AppResult extends Result {
    private final AppPojo appPojo;
    private final ComponentName className;
    private Drawable icon = null;

    AppResult(AppPojo appPojo) {
        super(appPojo);
        this.appPojo = appPojo;


        className = new ComponentName(appPojo.packageName, appPojo.activityName);
    }

    @Override
    public View display(final Context context, int position, View convertView) {
        View view = convertView;
        if (convertView == null) {
            view = inflateFromId(context, R.layout.item_app);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);


        TextView appName = view.findViewById(R.id.item_app_name);
        appName.setText(enrichText(appPojo.displayName, context));

        TextView tagsView = view.findViewById(R.id.item_app_tag);
        //Hide tags view if tags are empty or if user has selected to hide them when query doesn't match
        if (appPojo.displayTags.isEmpty() ||
                (!prefs.getBoolean("tags-visible", true) && !appPojo.displayTags.contains("{"))) {
            tagsView.setVisibility(View.GONE);
        } else {
            tagsView.setVisibility(View.VISIBLE);
            tagsView.setText(enrichText(appPojo.displayTags, context));
        }

        final ImageView appIcon = view.findViewById(R.id.item_app_icon);
        if (!prefs.getBoolean("icons-hide", false)) {
            if (appIcon.getTag() instanceof ComponentName && className.equals(appIcon.getTag())) {
                icon = appIcon.getDrawable();
            }
            this.setAsyncDrawable(appIcon);
        } else {
            appIcon.setImageDrawable(null);
        }
        return view;
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        if ((!(context instanceof MainActivity)) || (((MainActivity) context).isViewingSearchResults())) {
            adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        }
        adapter.add(new ListPopup.Item(context, R.string.menu_exclude));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        adapter.add(new ListPopup.Item(context, R.string.menu_tags_edit));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_app_details));
        adapter.add(new ListPopup.Item(context, R.string.menu_add_locked));

        ListPopup menu = inflatePopupMenu(adapter, context);

        try {
            // app installed under /system can't be uninstalled
            boolean isSameProfile = true;
            ApplicationInfo ai;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                LauncherActivityInfo info = launcher.getActivityList(this.appPojo.packageName, this.appPojo.userHandle.getRealHandle()).get(0);
                ai = info.getApplicationInfo();

                isSameProfile = this.appPojo.userHandle.isCurrentUser();
            } else {
                ai = context.getPackageManager().getApplicationInfo(this.appPojo.packageName, 0);
            }

            // Need to AND the flags with SYSTEM:
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && isSameProfile) {
                adapter.add(new ListPopup.Item(context, R.string.menu_app_uninstall));
            }
        } catch (NameNotFoundException | IndexOutOfBoundsException e) {
            // should not happen
        }

        //append root menu if available
        if (KissApplication.getApplication(context).getRootHandler().isRootActivated() && KissApplication.getApplication(context).getRootHandler().isRootAvailable()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_app_hibernate));
        }
        return menu;
    }

    @Override
    protected Boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId) {
        switch (stringId) {
            case R.string.menu_app_details:
                launchAppDetails(context, appPojo);
                return true;
            case R.string.menu_app_uninstall:
                launchUninstall(context, appPojo);
                return true;
            case R.string.menu_app_hibernate:
                hibernate(context, appPojo);
                return true;
            case R.string.menu_exclude:
                // remove item since it will be hidden
                parent.removeResult(this);
                excludeFromAppList(context, appPojo);
                return true;
            case R.string.menu_tags_edit:
                launchEditTagsDialog(context, appPojo);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId);
    }

    private void excludeFromAppList(Context context, AppPojo appPojo) {
        KissApplication.getApplication(context).getDataHandler().addToExcluded(appPojo.packageName, appPojo.userHandle);
        //remove app pojo from appProvider results - no need to reset handler
        KissApplication.getApplication(context).getDataHandler().getAppProvider().removeApp(appPojo);
        KissApplication.getApplication(context).getDataHandler().removeFromFavorites((MainActivity) context, appPojo.id);
        Toast.makeText(context, R.string.excluded_app_list_added, Toast.LENGTH_LONG).show();

    }


    private void launchEditTagsDialog(final Context context, final AppPojo app) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.tags_add_title));

        // Create the tag dialog
        final View v = LayoutInflater.from(context).inflate(R.layout.tags_dialog, null);
        final MultiAutoCompleteTextView tagInput = v.findViewById(R.id.tag_input);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, KissApplication.getApplication(context).getDataHandler().getTagsHandler().getAllTagsAsArray());
        tagInput.setTokenizer(new SpaceTokenizer());
        tagInput.setText(appPojo.getTags());

        tagInput.setAdapter(adapter);
        builder.setView(v);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Refresh tags for given app
                app.setTags(tagInput.getText().toString());
                KissApplication.getApplication(context).getDataHandler().getTagsHandler().setTags(app.id, app.getTags());
                // TODO: update the displayTags with proper highlight
                app.displayTags = app.getTags();
                // Show toast message
                String msg = context.getResources().getString(R.string.tags_confirmation_added);
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();
    }

    /**
     * Open an activity displaying details regarding the current package
     */
    private void launchAppDetails(Context context, AppPojo app) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;
            launcher.startAppDetailsActivity(className, appPojo.userHandle.getRealHandle(), null, null);
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", app.packageName, null));
            context.startActivity(intent);
        }
    }

    private void hibernate(Context context, AppPojo app) {
        String msg = context.getResources().getString(R.string.toast_hibernate_completed);
        if (!KissApplication.getApplication(context).getRootHandler().hibernateApp(appPojo.packageName)) {
            msg = context.getResources().getString(R.string.toast_hibernate_error);
        }

        Toast.makeText(context, String.format(msg, app.getName()), Toast.LENGTH_SHORT).show();
    }

    /**
     * Open an activity to uninstall the current package
     */
    private void launchUninstall(Context context, AppPojo app) {
        Intent intent = new Intent(Intent.ACTION_DELETE,
                Uri.fromParts("package", app.packageName, null));
        context.startActivity(intent);
    }

    @Override
    boolean isDrawableCached() {
        return icon != null;
    }

    @Override
    void setDrawableCache(Drawable drawable) {
        icon = drawable;
    }

    @Override
    public Drawable getDrawable(Context context) {
        synchronized (this) {
            if (icon == null) {
                icon = KissApplication.getApplication(context).getIconsHandler()
                        .getDrawableIconForPackage(className, this.appPojo.userHandle);
            }

            return icon;
        }
    }

    private boolean isInLockedList(String app, Context context) {
        String lockedApps = PreferenceManager.getDefaultSharedPreferences(context).getString("locked-apps-list", "");

        if(DataHolder.getInstance().getFirstTime() == true) {
            KissApplication.getApplication(context).getDataHandler().addToLocked((MainActivity) context, "app://com.android.chrome/com.google.android.apps.chrome.Main");
            KissApplication.getApplication(context).getDataHandler().addToLocked((MainActivity) context, "app://com.facebook.katana/com.facebook.katana.LoginActivity");
            KissApplication.getApplication(context).getDataHandler().addToLocked((MainActivity) context, "app://com.google.android.gm/com.google.android.gm.ConversationListActivityGmail");
            KissApplication.getApplication(context).getDataHandler().addToLocked((MainActivity) context, "app://com.google.android.apps.messaging/com.google.android.apps.messaging.ui.ConversationListActivity");
            KissApplication.getApplication(context).getDataHandler().addToLocked((MainActivity) context, "app://com.facebook.orca/com.facebook.orca.auth.StartScreenActivity");
            KissApplication.getApplication(context).getDataHandler().addToLocked((MainActivity) context, "app://com.google.android.youtube/com.google.android.youtube.app.honeycomb.Shell$HomeActivity");
            KissApplication.getApplication(context).getDataHandler().addToLocked((MainActivity) context, "app://com.whatsapp/com.whatsapp.Main");
            KissApplication.getApplication(context).getDataHandler().addToLocked((MainActivity) context, "app://com.android.dialer/com.android.dialer.DialtactsActivity");
            DataHolder.getInstance().setFirstTime(false);
        }

        return lockedApps.contains(app + ";");
    }

    @Override
    public void doLaunch(Context context, View v) {
        try {
            Log.d("APP_NAME", className.getClassName());
            boolean isInList = isInLockedList(className.getClassName(), context);
            boolean locked = true;
            if(DataHolder.getInstance().getUsb() == true) {
                if(DataHolder.getInstance().getFingers() < 5 && DataHolder.getInstance().getCarLocation().value <=5) {
                    locked = true;
                }else {
                    if(DataHolder.getInstance().getCarLocation().value == 2) {
                        locked = true;
                    }else {
                        locked = false;
                    }
                }
            }else {
                if(DataHolder.getInstance().getCarLocation().value <=5) {
                    locked = true;
                } else {
                    locked = false;
                }
            }

            float speed = DataHolder.getInstance().getSpeed();
            boolean stopped = speed <= 10;

            if (stopped || !isInList || (isInList && !locked)) {
                launchApp(context, v);
            } else {
                // 1. Instantiate an AlertDialog.Builder with its constructor
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                // 2. Chain together various setter methods to set the dialog characteristics
                //builder.setMessage(R.string.dialog_message).setTitle(R.string.dialog_title);
                builder.setMessage("It looks like you are in front of the car going at: " + Float.toString(speed) + " mph and you have " + DataHolder.getInstance().getFingers() + " fingers. Please don't text and drive.").setTitle(R.string.dialog_title);
                // 3. Get the AlertDialog launchAppfrom create()
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        } catch (ActivityNotFoundException | NullPointerException e) {
            // Application was just removed?
            // (null pointer exception can be thrown on Lollipop+ when app is missing)
            Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
        }
    }

    private void launchApp(Context context, View v) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;
            launcher.startMainActivity(className, appPojo.userHandle.getRealHandle(), v.getClipBounds(), null);
        } else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(className);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                intent.setSourceBounds(v.getClipBounds());
            }

            context.startActivity(intent);
        }
    }
}
