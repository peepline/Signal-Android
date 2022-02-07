package org.thoughtcrime.securesms.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Stream;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.thoughtcrime.securesms.ContactSelectionActivity;
import org.thoughtcrime.securesms.ContactSelectionListFragment;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.util.FeatureFlags;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.concurrent.SimpleTask;

import java.util.List;


public class ContactManagerActivity extends ContactSelectionActivity {

  private static final short REQUEST_CODE_ADD_DETAILS = 17275;
  public static String IS_CONTACT_MANAGER = "is_contact_manager";

  private ExtendedFloatingActionButton next;
  private SeekBar                      trustSeekBar;
  private TextView                     trustLevelHeading;

  private double                       trustLevel;

  public static Intent newIntent(@NonNull Context context) {
    Intent intent = new Intent(context, ContactManagerActivity.class);

    intent.putExtra(ContactSelectionListFragment.REFRESHABLE, false);
    intent.putExtra(ContactSelectionActivity.EXTRA_LAYOUT_RES_ID, R.layout.contact_manager_activity);

    int displayMode = Util.isDefaultSmsProvider(context) ? ContactsCursorLoader.DisplayMode.FLAG_SMS | ContactsCursorLoader.DisplayMode.FLAG_PUSH
                                                         : ContactsCursorLoader.DisplayMode.FLAG_PUSH;

    intent.putExtra(ContactSelectionListFragment.DISPLAY_MODE, displayMode);
    intent.putExtra(ContactSelectionListFragment.SELECTION_LIMITS, FeatureFlags.groupLimits().excludingSelf());
    intent.putExtra(IS_CONTACT_MANAGER, true);

    return intent;
  }

  @Override
  public void onCreate(Bundle bundle, boolean ready) {
    super.onCreate(bundle, ready);
    assert getSupportActionBar() != null;
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    trustLevel = 0;

    next = findViewById(R.id.next);
    next.setOnClickListener(v -> handleNextPressed());

    trustLevelHeading = findViewById(R.id.trustLevelHeading);

    trustSeekBar = (SeekBar)findViewById(R.id.trustSeekBar);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      trustSeekBar.setTooltipText("20");
    }

    trustSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        trustLevel = convertSeekbarValue(progress);
        trustLevelHeading.setText(getResources().getString(R.string.ContactManagerActivity__trust_level_heading) + " " + String.format("%.1f", trustLevel));
      }

      @Override public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });
  }

  private double convertSeekbarValue(int value){
    return value * 0.1f;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == REQUEST_CODE_ADD_DETAILS && resultCode == RESULT_OK) {
      finish();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onSelectionChanged() {
  }

  private void handleNextPressed() {
    ContactAccessor contactAccessor = ContactAccessor.getInstance();

    List<RecipientId> ids = Stream.of(contactsFragment.getSelectedContacts())
                                  .map(selectedContact -> selectedContact.getOrCreateRecipientId(this))
                                  .toList();

    Context context = this;

    SimpleTask.BackgroundTask backgroundTask = new SimpleTask.BackgroundTask(){
      @Override public Object run() {
          for (RecipientId recipient_id : ids) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(ContactsContract.Data.DATA2, trustLevel);
            contactAccessor.addOrUpdateContactData(context, (int) recipient_id.toLong(), contentValues);
          }
          return null;
        }
    };

    SimpleTask.ForegroundTask foregroundTask = new SimpleTask.ForegroundTask() {
      @Override public void run(Object result) {

      }
    };

    SimpleTask.run(getLifecycle(), backgroundTask , foregroundTask);

    this.onBackPressed();
  }
}
