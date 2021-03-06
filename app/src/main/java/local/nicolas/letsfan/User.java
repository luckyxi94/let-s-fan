package local.nicolas.letsfan;

import android.provider.ContactsContract;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

/**
 * Created by soshy on 20/12/2016.
 */

public class User {
    private String firstName;
    private String lastName;
    private String nickName;
    private String email;
    private Double tasteVariation;
    private TasteVector tasteVector;
    private Boolean isInfoPublic;

    public User(String _nick_name, String _first_name, String _last_name, String _email, Double _taste_variation, Double _taste_sour, Double _taste_sweet, Double _taste_bitter, Double _taste_spice, Double _taste_salty, Boolean _is_public) {
        nickName = _nick_name;
        firstName = _first_name;
        lastName = _last_name;
        email = _email;
        tasteVariation = _taste_variation;
        isInfoPublic = _is_public;
        tasteVector = new TasteVector(_taste_sour, _taste_sweet, _taste_bitter, _taste_spice, _taste_salty);
    }

    public void createUserInDatabase (FirebaseDatabase db, String uid) {
//        userRef.child(uid).child("firstName").setValue(firstName);
//        userRef.child(uid).child("lastName").setValue(lastName);
//        userRef.child(uid).child("nickName").setValue(nickName);
//        userRef.child(uid).child("email").setValue(email);
//        userRef.child(uid).child("tasteVariation").setValue(tasteVariation);
//        userRef.child(uid).child("isInfoPublic").setValue(isInfoPublic);
//        userRef.child(uid).child("tasteVector").child("sour").setValue(tasteVector.sour);
//        userRef.child(uid).child("tasteVector").child("sweet").setValue(tasteVector.sweet);
//        userRef.child(uid).child("tasteVector").child("bitter").setValue(tasteVector.bitter);
//        userRef.child(uid).child("tasteVector").child("spice").setValue(tasteVector.spice);
//        userRef.child(uid).child("tasteVector").child("salty").setValue(tasteVector.salty);
        db.getReference("users").child(uid).setValue(this);
    }

    public void createInvitation (FirebaseDatabase db, String uid, String _startTime, String _endTime, String _date, String _restaurant) {

        // invitation
        DatabaseReference inviRef = db.getReference("invitations");
        DatabaseReference currentRef = inviRef.push();
        currentRef.setValue(new Invitation(_date, uid, this.tasteVariation, _restaurant, _startTime, _endTime));
        String pushID = currentRef.getKey();

        // index on invitationAttendees
        DatabaseReference invAttendeeRef = db.getReference("invitationAttendees");
        invAttendeeRef.child(pushID).child(uid).child("tasteVector").setValue(tasteVector);
        invAttendeeRef.child(pushID).child(uid).child("startTime").setValue(_startTime);
        invAttendeeRef.child(pushID).child(uid).child("endTime").setValue(_endTime);

        // index on userInEvents
        DatabaseReference userInEventsRef = db.getReference("userInEvents");
        userInEventsRef.child(uid).child(pushID).child("date").setValue(_date);
        userInEventsRef.child(uid).child(pushID).child("organizerName").setValue(nickName);
    }
}

class TasteVector {
    protected Double sour;
    protected Double sweet;
    protected Double bitter;
    protected Double spice;
    protected Double salty;
    public TasteVector(Double _sour, Double _sweet, Double _bitter, Double _spice, Double _salty) {
        sour = _sour; sweet = _sweet; bitter = _bitter; spice = _spice; salty = _salty;
    }
}
