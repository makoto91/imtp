/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imap;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;
import java.io.*;
import java.io.BufferedReader;

/**
 *
 * @author matija.veljkovic
 */
public class Imap {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        InputStreamReader sr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(sr);
        //unos username-a i sifre
        System.out.println("Unesite username:");
        String username = "";

        try {
            username = br.readLine();
        } catch (IOException ex) {
            System.out.println("Username nije unet na odgovarajuci nacin!");
        }

        System.out.println("Unesite sifru:");
        String password = "";
        try {
            password = br.readLine();
        } catch (IOException ex) {
            System.out.println("Sifra nije uneta na odgovarajuci nacin!");
        }
        //postavljanje hosta, provajdera i eneble-ovanje ssl-a
        props.setProperty("mail.imap.ssl.enable", "true");
        String host = "imap.gmail.com";
        String provider = "imap";

        try {
            //Konekcija na server sa prikupljenim podacima
            Session session = Session.getDefaultInstance(props);
            Store store = session.getStore(provider);
            store.connect(host, username, password);

            
            
            //petlja za trajanje aplikacije, na logout se radi break

            while (true) {

                System.out.println("Unesite id foldera koji zelite da vidite. Ako zelite da se izlogujete, unesite -1. Folderi koje imate su:");

                //povlacenje svih foldera u niz i njihov ispis zajedno sa id-jevima po kojima im se pristupa
                javax.mail.Folder[] folders = store.getDefaultFolder().list("*");
                for (int i = 0; i < folders.length; i++) {
                    if ((folders[i].getType() & javax.mail.Folder.HOLDS_MESSAGES) != 0) {
                        System.out.println(folders[i].getFullName() + " - Broj poruka:  " + folders[i].getMessageCount() + " ID : " + i);
                    }
                }
                //logout, pristup folderu ili obrada greske i vracanje na odabir foldera
                int folderId = 0;
                try {
                    folderId = Integer.parseInt(br.readLine());
                    if (folderId == -1) {
                        //logout
                        store.close();
                        System.out.println("Dovidjenja!");
                        break;
                    } else if (folderId < -1 || folderId >= folders.length) {
                        System.err.println("Uneli ste nepostojeci id!");
                        continue;
                    }
                } catch (IOException ex) {
                    System.out.println("Naziv foldera nije unet na odgovarajuci nacin!");
                }
                //pristup folderu sa odgovarajucim id-jem
                Folder folder = folders[folderId];
                folder.open(Folder.READ_ONLY);

                // cuvanje poruka iz foldera u nizu
                Message[] messages = folder.getMessages();

                for (int i = 0; i < messages.length; i++) {
                    //ispis rednog broja poruke i from-a pozivanje nase funkcije getFrom
                    System.out.println("\n\t\tPoruka " + (i + 1) + "\nFrom: " + getFrom(messages[i]));
                    //ispisivanje subjecta
                    System.out.println("Subject: " + messages[i].getSubject());
                    //ispis body-ja pozivanje f-je getTextFromMessage
                    System.out.println("Body: " + getTextFromMessage(messages[i]));

                }

                //zatvaranje foldera
                folder.close(false);
            }

        } catch (NoSuchProviderException nspe) {
            System.err.println("invalid provider name");
        } catch (AuthenticationFailedException afe) {
            System.err.println("Pogresno su uneti username ili password!");
        } catch (IOException ioe) {
            System.err.println(ioe);
        } catch (MessagingException me) {
            System.err.println(me);
        }

    }

    private static String getFrom(Message javaMailMessage)
            throws MessagingException {

        String from = "";
        //getFrom vraca niz
        Address a[] = javaMailMessage.getFrom();

        if (a == null) {
            return null;
        }
        //prolazimo i concatenatujemo clanove niza
        for (int i = 0; i < a.length; i++) {
            Address address = a[i];
            from = from + address.toString();
        }
        //izbacivanje navodnika iz froma ako postoje
        if (from.contains("\"")) {
            from = from.replace("\"", "");
        };

        return from;
    }

    private static String getTextFromMessage(Message message) throws Exception {
        String result = "";
        //ako poruka nije multipart, tj jeste text/plain odmah vracam ispis
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } //u suprotnom se kastuje u multipart i prosledjuje f-ji getTextFromMimeMultipart
        else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private static String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart) throws Exception {
        String result = "";
        //prolazimo kroz sve njene delove i izlazimo iz fora cim dodjemo do text/plain ciji ispis vracamo.
        //Jedino nam je on bitan za ispis u konzoli, ako bi proveravali dalje mogli bi naici na text/html
        // ili bilo sta sto se moze naci u attachmentu  -- link za spisak ako te zanima https://www.sitepoint.com/web-foundations/mime-types-complete-list/
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break;
            }
        }
        return result;
    }

}
