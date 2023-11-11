package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;

  //
  // Instance variables
  //

  protected Query() throws SQLException, IOException {
    prepareStatements();
  }

  /**
   * Clear the data in any custom tables created.
   *
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      // TODO: YOUR CODE HERE
      String[] tables = { "Reservations", "Users" };
      for (String s : tables) {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DELETE FROM " + s + ";");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);

    // TODO: YOUR CODE HERE
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
    try {
      String usersQuery = "SELECT * FROM Users WHERE Username = \'" + username + "\' AND Password = \'"
          + password + "\';";
      Statement usersStatement = conn.createStatement();
      ResultSet usersResults = usersStatement.executeQuery(usersQuery);
      if (usersResults.next()) {
        String loggedInQuery = "SELECT LoggedIn FROM Users WHERE Username = \'" + username + "\' AND Password = \'"
            + password + "\';";
        Statement loggedInStatement = conn.createStatement();
        ResultSet loggedInResult = loggedInStatement.executeQuery(loggedInQuery);
        // int result_dayOfMonth = oneHopResults.getInt("day_of_month");
        int loggedIn = loggedInResult.getInt("LoggedIn");
        loggedInResult.close();
        usersResults.close();
        if (loggedIn == 1) {
          return "User already logged in\n";
        } else {
          // change the LoggedIn field
          String loginUpdateQuery = "UPDATE Users SET LoggedIn = 1 WHERE Username = \'" + username
              + "\' AND Password = \';"
              + password + "\'";
          Statement updateLoginStatement = conn.createStatement();
          updateLoginStatement.executeUpdate(loginUpdateQuery);
          return "Logged in as " + username + '\n';
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "Login failed \n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    if (initAmount < 0) {
      return "Failed to create user\n";
    }
    try {
      String uuid = UUID.randomUUID().toString();
      String insertQuery = "INSERT INTO Users (UserID, Username, Password) VALUES (?, ?, ?);";
      PreparedStatement preparedStatement = conn.prepareStatement(insertQuery);
      preparedStatement.setString(1, uuid);
      preparedStatement.setString(2, username);
      preparedStatement.setString(3, password);

      int affectedRows = preparedStatement.executeUpdate();

      if (affectedRows == 0) {
        throw new SQLException("Creating user failed, no rows affected");
      }

      return "Created user " + username + "\n";

    } catch (Exception e) {
      System.out.println("Failed to create user\n");
      e.printStackTrace();
    }
    return "Failed to create user\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_search(String originCity, String destinationCity,
      boolean directFlight, int dayOfMonth,
      int numberOfItineraries) {

    StringBuffer sb = new StringBuffer();

    try {
      String searchString;

      PreparedStatement preparedStatement;

      if (directFlight) {
        searchString = "SELECT TOP (?) day_of_month, carrier_id, flight_num, fid, "
            + "origin_city, dest_city, actual_time, capacity, price FROM Flights WHERE "
            + "origin_city = (?) AND dest_city = (?) AND day_of_month = (?) AND canceled = 0 ORDER BY "
            + "actual_time ASC";

        preparedStatement = conn.prepareStatement(searchString);

        preparedStatement.setInt(1, numberOfItineraries);
        preparedStatement.setString(2, originCity);
        preparedStatement.setInt(3, dayOfMonth);

      } else {
        searchString = "SELECT TOP (?) * FROM ( " +
            "SELECT f1.day_of_month, "
            + "f1.fid AS fid1, f2.fid AS fid2 "
            + "FROM FLIGHTS AS f1 "
            + "JOIN FLIGHTS AS f2 ON f1.dest_city = f2.origin_city AND f1.day_of_month = f2.day_of_month "
            + "WHERE f1.origin_city = ? AND f2.dest_city = ? "
            + "AND f1.day_of_month = ? "
            + "AND f1.canceled = 0 AND f2.canceled = 0 "
            + "UNION "
            + "SELECT "
            + "fid AS fid1, NULL AS fid2, "
            + "FROM FLIGHTS "
            + "WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? AND canceled = 0)"
            + "AS combined_results ORDER BY total_time ASC";

        preparedStatement = conn.prepareStatement(searchString);

        preparedStatement.setInt(1, numberOfItineraries);
        preparedStatement.setString(2, originCity);
        preparedStatement.setString(3, destinationCity);
        preparedStatement.setInt(4, dayOfMonth);

        preparedStatement.setString(5, originCity);
        preparedStatement.setString(6, destinationCity);
        preparedStatement.setInt(7, dayOfMonth);
      }

      ResultSet results = preparedStatement.executeQuery();

      if (!results.isBeforeFirst()) {
        return "No flights match your selection\n";
      }

      int counter = 0;

      while (results.next()) {
        int f1_fid = results.getInt("fid1");
        int f2_fid = results.getInt("fid2");

        if (!directFlight) {
          sb.append("Itinerary " + counter + ": 2 flight(s), " + results.getInt("total_time") + " minutes\n");
          Flight f1 = new Flight(f1_fid, dayOfMonth, getCarrierId(f1_fid),
              getFlightNum(f1_fid), originCity, getOriginCity(f1_fid),
              getTime(f1_fid), checkFlightCapacity(f1_fid),
              getPrice(f1_fid));
        } else {
          sb.append("Itinerary " + counter + ": 1 flight(s), " + results.getInt("actual_time") + " minutes\n");
        }

        counter++;

      }

      results.close();

    } catch (SQLException e) {
      e.printStackTrace();
      return "Failed to search\n";
    }

    return sb.toString();
  }

  /**
   * Searches for the flight and gives the price for that flight
   *
   * @param fid the flight id
   * @return the price for that flight
   */
  private int getPrice(int fid) {
    try {
      String priceQuery = "SELECT price FROM Flights WHERE fid = " + fid + ";";
      Statement priceStatement = conn.createStatement();
      ResultSet priceResult = priceStatement.executeQuery(priceQuery);
      int price = priceResult.getInt("price");
      priceResult.close();
      return price;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  /**
   * Searches for the specific flight and gives the origin city for that flight
   *
   * @param fid the flight id
   * @return the origin city for that flight
   */
  private String getOriginCity(int fid) {
    try {
      String originCityQuery = "SELECT origin_city FROM Flights WHERE fid = " + fid + ";";
      Statement originCityStatement = conn.createStatement();
      ResultSet originCityResult = originCityStatement.executeQuery(originCityQuery);
      String originCity = originCityResult.getString("origin_city");
      originCityResult.close();
      return originCity;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Searches for the specific flight and gives the carrier for that flight
   *
   * @param fid the flight id
   * @return the carrier for that flight
   */
  private String getCarrierId(int fid) {
    try {
      String carrierIdQuery = "SELECT carrier_id FROM Flights WHERE fid = " + fid + ";";
      Statement carrierIdStatement = conn.createStatement();
      ResultSet carrierIdResult = carrierIdStatement.executeQuery(carrierIdQuery);
      String carrierId = carrierIdResult.getString("carrier_id");
      carrierIdResult.close();
      return carrierId;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Searches for the specific flight and gives the flight_num
   *
   * @param fid the flight id
   * @return the flight_num of the flight
   */
  private String getFlightNum(int fid) {
    try {
      String flightNumQuery = "SELECT flight_num FROM Flights WHERE fid = " + fid + ";";
      Statement flightNumStatement = conn.createStatement();
      ResultSet flightNumResult = flightNumStatement.executeQuery(flightNumQuery);
      String flightNum = flightNumResult.getString("flight_num");
      flightNumResult.close();
      return flightNum;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Searches for the specific flight and gives the total time the flight takes
   *
   * @param fid the flight id
   * @return the total time the flight takes
   */
  private int getTime(int fid) {
    try {
      String timeQuery = "SELECT actual_time FROM Flights WHERE fid = " + fid + ";";
      Statement timeStatement = conn.createStatement();
      ResultSet timeResult = timeStatement.executeQuery(timeQuery);
      int time = timeResult.getInt("actual_time");
      timeResult.close();
      return time;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_book(int itineraryId) {
    // TODO: YOUR CODE HERE
    return "Booking failed\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_pay(int reservationId) {
    // TODO: YOUR CODE HERE
    return "Failed to pay for reservation " + reservationId + "\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE
    return "Failed to retrieve reservations\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   *
   * TODO(hctang): move this into QueryAbstract
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
        int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }

    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }
}
