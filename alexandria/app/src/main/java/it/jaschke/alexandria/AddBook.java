package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BasicScannerActivity;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private EditText ean;
    private final int LOADER_ID = 1;
    private String toastString;
    private View rootView;
    private final String EAN_CONTENT="eanContent";
    private static final int SCAN_IMAGE_REQUEST = 1;

    private Button deleteButton;
    private Button saveButton;
    private boolean scanComplete;
    private SharedPreferences sharedPreferences;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        clearFields();
        Log.v(getClass().getSimpleName(), "Inside onsharedpreferences changed");
        @BookService.ConnectionStatus int networkStatus = sharedPreferences.getInt(getString(R.string.pref_connectivity_key),0);
        switch (networkStatus) {
            case BookService.SERVER_STATUS_DOWN:
                toastString = getString(R.string.server_status_down);
                break;
            case BookService.CLIENT_STATUS_DOWN:
                toastString = getString(R.string.client_status_down);
                break;
            default:
                return;
        }

        Toast.makeText(getContext(),toastString,Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(ean!=null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        deleteButton = (Button)rootView.findViewById(R.id.delete_button);
        saveButton = (Button)rootView.findViewById(R.id.save_button);

        ean = (EditText) rootView.findViewById(R.id.ean);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean = s.toString();
                String prefix = getString(R.string.isbn_prefix);
                if (ean.length() > 0) {
                    //catch isbn10 numbers
                    if (ean.length() == 10 && !ean.startsWith(prefix)) {
                        ean = prefix + ean;
                    }

                    if (ean.length() < 13) {
                        if (scanComplete) {
                            Toast.makeText(getContext(), getString(R.string.isbn_wrong_format), Toast.LENGTH_SHORT).show();
                        }
                        clearFields();
                    } else {

                        //Once we have an ISBN, start a book intent
                        sendBookIntent(BookService.FETCH_BOOK);
                        AddBook.this.restartLoader();
                    }
                    scanComplete = false;
                }
            }
        });

        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getActivity();
                Intent scanIntent = new Intent(context, BasicScannerActivity.class);
                startActivityForResult(scanIntent, SCAN_IMAGE_REQUEST);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ean.setText("");
                deleteButton.setVisibility(View.INVISIBLE);
                saveButton.setVisibility(View.INVISIBLE);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBookIntent(BookService.DELETE_BOOK);
                ean.setText("");
                deleteButton.setVisibility(View.INVISIBLE);
                saveButton.setVisibility(View.INVISIBLE);
                restartLoader();
            }
        });

        if(savedInstanceState!=null){
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void sendBookIntent(String action){
        Intent bookIntent = new Intent(getActivity(),BookService.class);
        bookIntent.putExtra(BookService.EAN, ean.getText().toString());
        bookIntent.setAction(action);
        getActivity().startService(bookIntent);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(getClass().getSimpleName(), "Result from Scan activity" + String.valueOf(requestCode) + "," + String.valueOf(resultCode));
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SCAN_IMAGE_REQUEST){
            if(resultCode == BasicScannerActivity.SCAN_OK){
                scanComplete = true;
                if(data!=null) {
                    ean.setText(data.getStringExtra(Intent.EXTRA_TEXT));
                }else{
                    ean.setText("");
                }
            }
        }
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(ean.getText().length()==0){
            return null;
        }
        String eanStr= ean.getText().toString();
        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        if(authors!=null) {
            String[] authorsArr = authors.split(",");
            ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
            ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",", "\n"));
        } else{
            ((TextView) rootView.findViewById(R.id.authors)).setLines(0);
        }

        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
            rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

        deleteButton.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }
}
