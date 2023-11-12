package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
      String[] tables = { "Reservations_lshibly", "Users_lshibly" };
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

  private String currentLoggedInUser = null;

  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
    try {
      if (currentLoggedInUser != null) {
        return "User already logged in\n";
      }
      String usersQuery = "SELECT * FROM Users_lshibly WHERE Username = ? AND Password = ?;";
      PreparedStatement usersStatement = conn.prepareStatement(usersQuery);
      usersStatement.setString(1, username);
      usersStatement.setString(2, password);
      ResultSet usersResults = usersStatement.executeQuery();
      if (usersResults.next()) {
        int loggedIn = usersResults.getInt("LoggedIn");
        if (loggedIn == 1) {
          usersResults.close();
          return "User already logged in\n";
        } else {
          // change the LoggedIn field
          String loginUpdateQuery = "UPDATE Users_lshibly SET LoggedIn = 1 WHERE Username = ?;";
          PreparedStatement updateLoginStatement = conn.prepareStatement(loginUpdateQuery);
          updateLoginStatement.setString(1, username);
          updateLoginStatement.executeUpdate();
          usersResults.close();
          currentLoggedInUser = username;
          return "Logged in as " + username + '\n';
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "Login failed\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    if (initAmount < 0) {
      return "Failed to create user\n";
    }
    try {
      String insertQuery = "INSERT INTO Users_lshibly (Username, Password) VALUES (?, ?);";
      PreparedStatement preparedStatement = conn.prepareStatement(insertQuery);
      preparedStatement.setString(1, username);
      preparedStatement.setString(2, password);

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

    List<Flight> directFlights = new ArrayList<>();
    List<List<Flight>> indirectFlightItin = new ArrayList<>();

    try {
      String searchString = "SELECT TOP (?) fid AS fid1, NULL AS fid2, actual_time AS total_time "
          + "FROM FLIGHTS "
          + "WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? AND canceled = 0 "
          + "ORDER BY total_time ASC";
      PreparedStatement preparedStatement = conn.prepareStatement(searchString);
      preparedStatement.setInt(1, numberOfItineraries);
      preparedStatement.setString(2, originCity);
      preparedStatement.setString(3, destinationCity);
      preparedStatement.setInt(4, dayOfMonth);

      ResultSet flightSet = preparedStatement.executeQuery();

      int num = 0;

      while (flightSet.next()) {
        // populate directFlights
        int fid = flightSet.getInt("fid1");
        Flight f = new Flight(fid, dayOfMonth, getCarrierId(fid), getFlightNum(fid), originCity,
            destinationCity, getTime(fid), checkFlightCapacity(fid), getPrice(fid));

        directFlights.add(f);

        num++;
      }
      flightSet.close();

      if (!directFlight) {

        if (num < numberOfItineraries) {
          System.out.println(directFlights.toString());
          // this means that we didn't query numberOfItineraries' worth of
          // itineraries. So now we are querying to indirect flights as well
          String indirectSearch = "SELECT TOP (?) fid1, fid2, total_time FROM ("
              + "SELECT "
              + "f1.fid AS fid1, f2.fid AS fid2, f1.actual_time + f2.actual_time AS total_time "
              + "FROM FLIGHTS AS f1 "
              + "JOIN FLIGHTS AS f2 ON f1.dest_city = f2.origin_city AND f1.day_of_month = f2.day_of_month "
              + "WHERE f1.origin_city = ? AND f2.dest_city = ? "
              + "AND f1.day_of_month = ? "
              + "AND f1.canceled = 0 AND f2.canceled = 0) AS combined_results ORDER BY total_time ASC";

          PreparedStatement indirectStatement = conn.prepareStatement(indirectSearch);
          indirectStatement.setInt(1, numberOfItineraries - num);
          indirectStatement.setString(2, originCity);
          indirectStatement.setString(3, destinationCity);
          indirectStatement.setInt(4, dayOfMonth);

          ResultSet indirectFlightSet = indirectStatement.executeQuery();

          System.out.println("Hello");

          while (indirectFlightSet.next()) {
            // populate indirectFlightItin
            int fid1 = indirectFlightSet.getInt("fid1");
            int fid2 = indirectFlightSet.getInt("fid2");
            Flight f1 = new Flight(fid1, dayOfMonth, getCarrierId(fid1), getFlightNum(fid1), originCity,
                getDestCity(fid1), getTime(fid1), checkFlightCapacity(fid1), getPrice(fid1));

            Flight f2 = new Flight(fid2, dayOfMonth, getCarrierId(fid2), getFlightNum(fid2), getDestCity(fid1),
                destinationCity, getTime(fid2), checkFlightCapacity(fid2), getPrice(fid2));

            List<Flight> temp = new ArrayList<>();
            temp.add(f1);
            temp.add(f2);

            indirectFlightItin.add(temp);

          }

          indirectFlightSet.close();

          int i = 0;
          int j = 0;
          int counter = 0;

          while (i < directFlights.size() && j < indirectFlightItin.size()) {

            int directFlightTime = directFlights.get(i).time;
            int indirectFlightTime = indirectFlightItin.get(j).get(0).time
                + indirectFlightItin.get(j).get(1).time;

            if (directFlightTime < indirectFlightTime) {
              Flight f1 = directFlights.get(i);
              sb.append("Itinerary " + counter + ": 1 flight(s), " + directFlightTime
                  + " minutes\n" + f1.toString() + "\n");
              i++;
            } else {
              Flight f1 = indirectFlightItin.get(j).get(0);
              Flight f2 = indirectFlightItin.get(j).get(1);
              sb.append("Itinerary " + counter + ": 2 flight(s), " + indirectFlightTime
                  + " minutes\n" + f1.toString() + "\n" + f2.toString() + "\n");
              j++;
            }
            counter++;
          }

          // Handle remaining direct flights
          while (i < directFlights.size()) {
            Flight f1 = directFlights.get(i);
            int directFlightTime = f1.time;
            sb.append(
                "Itinerary " + counter + ": 1 flight(s), " + directFlightTime + " minutes\n" + f1.toString() + "\n");
            i++;
            counter++;
          }

          // Handle remaining indirect flights
          while (j < indirectFlightItin.size()) {
            Flight f1 = indirectFlightItin.get(j).get(0);
            Flight f2 = indirectFlightItin.get(j).get(1);
            int indirectFlightTime = f1.time + f2.time;
            sb.append("Itinerary " + counter + ": 2 flight(s), " + indirectFlightTime + " minutes\n" + f1.toString()
                + "\n" + f2.toString() + "\n");
            j++;
            counter++;
          }

        } else {
          int itinCounter = 0;
          for (Flight flight : directFlights) {
            Flight f = flight;
            sb.append("Itinerary " + itinCounter + ": 1 flight(s), " + f.time
                + " minutes\n" + f.toString() + "\n");
            itinCounter++;

          }
        }
      } else {
        int itinCounter = 0;
        for (Flight flight : directFlights) {
          Flight f = flight;
          sb.append("Itinerary " + itinCounter + ": 1 flight(s), " + f.time
              + " minutes\n" + f.toString() + "\n");
          itinCounter++;

        }
      }

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
      priceResult.next();
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
      originCityResult.next();
      String originCity = originCityResult.getString("origin_city");
      originCityResult.close();
      return originCity;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Searches for the specific flight and gives the origin city for that flight
   *
   * @param fid the flight id
   * @return the origin city for that flight
   */
  private String getDestCity(int fid) {
    try {
      String destCityQuery = "SELECT dest_city FROM Flights WHERE fid = " + fid + ";";
      Statement destCityStatement = conn.createStatement();
      ResultSet destCityResult = destCityStatement.executeQuery(destCityQuery);
      destCityResult.next();
      String originCity = destCityResult.getString("dest_city");
      destCityResult.close();
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
      String carrierIdQuery = "SELECT carrier_id FROM Flights WHERE fid = ?;";
      PreparedStatement carrierIdStatement = conn.prepareStatement(carrierIdQuery);
      carrierIdStatement.setInt(1, fid);
      ResultSet carrierIdResult = carrierIdStatement.executeQuery();
      carrierIdResult.next();
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
      flightNumResult.next();
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
      timeResult.next();
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

  class FlightComparator implements Comparator<Flight> {
    @Override
    public int compare(Flight f1, Flight f2) {
      return Integer.compare(f1.time, f2.time);
    }
  }

}
