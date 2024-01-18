package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static flightapp.PasswordUtils.*;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?;";
  private PreparedStatement flightCapacityStmt;

  private static final String CLEAR_USERS = "DELETE FROM Users_lshibly;";
  private PreparedStatement clearUsersStmt;

  private static final String CLEAR_RESERVATIONS = "DELETE FROM Reservations_lshibly;";
  private PreparedStatement clearReservationsStmt;

  private static final String SELECT_PASSWORDS = "SELECT Password FROM Users_lshibly WHERE Username = ?;";
  private PreparedStatement usersStmt;

  private static final String USER_CHECK = "SELECT password FROM Users_lshibly WHERE Username = ?;";
  private PreparedStatement userPrep;

  private static final String INSERT_USER = "INSERT INTO Users_lshibly (Username, Password, Balance) VALUES (?, ?, ?);";
  private PreparedStatement insertStmt;

  private static final String DIRECT_FLIGHT = "SELECT TOP (?) fid AS fid1, NULL AS fid2, actual_time AS total_time " +
      "FROM FLIGHTS " +
      "WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? AND canceled = 0 " +
      "ORDER BY total_time ASC, fid ASC";

  private PreparedStatement directStmt;

  private static final String INDIRECT_FLIGHTS = "SELECT TOP (?) fid1, fid2, total_time FROM (" +
      "SELECT " +
      "f1.fid AS fid1, f2.fid AS fid2, f1.actual_time + f2.actual_time AS total_time " +
      "FROM FLIGHTS AS f1 " +
      "JOIN FLIGHTS AS f2 ON f1.dest_city = f2.origin_city AND f1.day_of_month = f2.day_of_month " +
      "WHERE f1.origin_city = ? AND f2.dest_city = ? " +
      "AND f1.day_of_month = ? " +
      "AND f1.canceled = 0 AND f2.canceled = 0) AS combined_results " +
      "ORDER BY total_time ASC, fid1 ASC, fid2 ASC";

  private PreparedStatement indirectStmt;

  private static final String SELECT_PRICE = "SELECT price FROM Flights WHERE fid = ?;";
  private PreparedStatement priceStmt;

  private static final String SELECT_DAY = "SELECT day_of_month FROM Flights WHERE fid = ?;";
  private PreparedStatement dayStmt;

  private static final String SELECT_DEST = "SELECT dest_city FROM Flights WHERE fid = ?;";
  private PreparedStatement destStmt;

  private static final String SELECT_ORIGIN = "SELECT origin_city FROM Flights WHERE fid = ?;";
  private PreparedStatement originStmt;

  private static final String SELECT_CARRIER = "SELECT carrier_id FROM Flights WHERE fid = ?;";
  private PreparedStatement carrierStmt;

  private static final String SELECT_FLIGHT_NUM = "SELECT flight_num FROM Flights WHERE fid = ?;";
  private PreparedStatement flightNumStmt;

  private static final String SELECT_TIME = "SELECT actual_time FROM Flights WHERE fid = ?;";
  private PreparedStatement timeStmt;

  private static final String NUM_RESERVATIONS = "SELECT COUNT(*) AS num FROM Reservations_lshibly";
  private PreparedStatement resStmt;

  private static final String HAS_RESERVATION = "SELECT * FROM Reservations_lshibly WHERE Username = ? "
      + "AND fid1 = ? OR fid2 = ?;";
  private PreparedStatement hasStmt;

  private static final String TOTAL_RESERVATIONS = "SELECT COUNT(*) AS num FROM Reservations_lshibly WHERE "
      + "fid1 = ? OR fid2 = ?;";
  private PreparedStatement totalStmt;

  private static final String SELECT_USER_RESERVATION = "SELECT * FROM Reservations_lshibly WHERE"
      + " Username = ?;";
  private PreparedStatement userResStmt;

  private static final String INSERT_RESERVATION = "INSERT INTO Reservations_lshibly "
      + "(ReservationID, Username, fid1, fid2, status) "
      + "VALUES (?, ?, ?, ?, ?);";
  private PreparedStatement insertResStmt;

  private static final String SELECT_RES_WITH_ID = "SELECT * FROM Reservations_lshibly WHERE "
      + "Username = ? AND ReservationID = ?;";
  private PreparedStatement resIdStmt;

  private static final String SELECT_USER_BALANCE = "SELECT Balance FROM Users_lshibly WHERE Username = ?;";
  private PreparedStatement balanceStmt;

  private static final String UPDATE_USER_BALANCE = "UPDATE Users_lshibly SET Balance = ? WHERE Username = ?;";
  private PreparedStatement updateBalanceStmt;

  private static final String UPDATE_RESERVATION_STATUS = "UPDATE Reservations_lshibly SET status = 1 WHERE ReservationID = ?;";
  private PreparedStatement updateResStatusStmt;

  private static final String SELECT_RES = "SELECT * FROM Reservations_lshibly WHERE Username = ?;";
  private PreparedStatement selectResStmt;

  private Map<Integer, List<Flight>> itineraries;

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
      clearReservationsStmt.executeUpdate();
      clearUsersStmt.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);
    clearUsersStmt = conn.prepareStatement(CLEAR_USERS);
    clearReservationsStmt = conn.prepareStatement(CLEAR_RESERVATIONS);
    usersStmt = conn.prepareStatement(SELECT_PASSWORDS);
    userPrep = conn.prepareStatement(USER_CHECK);
    insertStmt = conn.prepareStatement(INSERT_USER);
    directStmt = conn.prepareStatement(DIRECT_FLIGHT);
    indirectStmt = conn.prepareStatement(INDIRECT_FLIGHTS);
    priceStmt = conn.prepareStatement(SELECT_PRICE);
    dayStmt = conn.prepareStatement(SELECT_DAY);
    destStmt = conn.prepareStatement(SELECT_DEST);
    originStmt = conn.prepareStatement(SELECT_ORIGIN);
    carrierStmt = conn.prepareStatement(SELECT_CARRIER);
    flightNumStmt = conn.prepareStatement(SELECT_FLIGHT_NUM);
    timeStmt = conn.prepareStatement(SELECT_TIME);
    resStmt = conn.prepareStatement(NUM_RESERVATIONS);
    hasStmt = conn.prepareStatement(HAS_RESERVATION);
    totalStmt = conn.prepareStatement(TOTAL_RESERVATIONS);
    userResStmt = conn.prepareStatement(SELECT_USER_RESERVATION);
    insertResStmt = conn.prepareStatement(INSERT_RESERVATION);
    resIdStmt = conn.prepareStatement(SELECT_RES_WITH_ID);
    balanceStmt = conn.prepareStatement(SELECT_USER_BALANCE);
    updateBalanceStmt = conn.prepareStatement(UPDATE_USER_BALANCE);
    updateResStatusStmt = conn.prepareStatement(UPDATE_RESERVATION_STATUS);
    selectResStmt = conn.prepareStatement(SELECT_RES);
  }

  private String currentLoggedInUser = null;

  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {

    if (currentLoggedInUser != null) {
      return "User already logged in\n";
    }

    try {
      usersStmt.clearParameters();
      usersStmt.setString(1, username);
      ResultSet usersResults = usersStmt.executeQuery();

      byte[] hash = null;

      while (usersResults.next()) {
        hash = usersResults.getBytes("Password");
      }

      usersResults.close();

      if (hash == null) {
        return "Login failed\n";
      }

      boolean passwordCheck = plaintextMatchesSaltedHash(password, hash);

      if (passwordCheck) {
        currentLoggedInUser = username;
        return "Logged in as " + username + "\n";
      } else {
        return "Login failed\n";
      }

    } catch (SQLException e) {
      e.printStackTrace();
      return "Login failed\n";
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    if (initAmount < 0) {
      return "Failed to create user\n";
    }
    try {

      // first check if the username already exists in the database
      userPrep.clearParameters();
      userPrep.setString(1, username);
      ResultSet user = userPrep.executeQuery();

      boolean failed = false;

      while (user.next()) {
        failed = true;
      }

      user.close();
      if (failed) {
        return "Failed to create user\n";
      }
    } catch (SQLException e) {
      e.printStackTrace();
      return "Failed to create user\n";
    }

    byte[] hash = saltAndHashPassword(password);

    try {
      insertStmt.clearParameters();
      insertStmt.setString(1, username);
      insertStmt.setBytes(2, hash);
      insertStmt.setInt(3, initAmount);
      insertStmt.executeUpdate();

      return "Created user " + username + "\n";

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "Failed to create user\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_search(String originCity, String destinationCity,
      boolean directFlight, int dayOfMonth,
      int numberOfItineraries) {

    if (numberOfItineraries < 0) {
      return "Failed to search\n";
    }

    StringBuffer sb = new StringBuffer();
    this.itineraries = new HashMap<>();

    List<Flight> directFlights = new ArrayList<>();
    List<List<Flight>> indirectFlightItin = new ArrayList<>();

    try {
      directStmt.setInt(1, numberOfItineraries);
      directStmt.setString(2, originCity);
      directStmt.setString(3, destinationCity);
      directStmt.setInt(4, dayOfMonth);

      ResultSet flightSet = directStmt.executeQuery();

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
          // this means that we didn't query numberOfItineraries' worth of
          // itineraries. So now we are querying to indirect flights as well
          indirectStmt.setInt(1, numberOfItineraries - num);
          indirectStmt.setString(2, originCity);
          indirectStmt.setString(3, destinationCity);
          indirectStmt.setInt(4, dayOfMonth);

          ResultSet indirectFlightSet = indirectStmt.executeQuery();

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
          if (indirectFlightItin.isEmpty() && num == 0) {
            return "No flights match your selection\n";
          }

          indirectFlightSet.close();

          int i = 0;
          int j = 0;
          int counter = 0;

          while (i < directFlights.size() && j < indirectFlightItin.size()) {

            List<Flight> mapValue = new ArrayList<>();

            int directFlightTime = directFlights.get(i).time;
            int indirectFlightTime = indirectFlightItin.get(j).get(0).time
                + indirectFlightItin.get(j).get(1).time;

            if (directFlightTime < indirectFlightTime) {
              Flight f1 = directFlights.get(i);
              mapValue.add(f1);
              itineraries.put(counter, mapValue);
              sb.append("Itinerary " + counter + ": 1 flight(s), " + directFlightTime
                  + " minutes\n" + f1.toString() + "\n");
              i++;
            } else {
              Flight f1 = indirectFlightItin.get(j).get(0);
              Flight f2 = indirectFlightItin.get(j).get(1);
              mapValue.add(f1);
              mapValue.add(f2);
              itineraries.put(counter, mapValue);
              sb.append("Itinerary " + counter + ": 2 flight(s), " + indirectFlightTime
                  + " minutes\n" + f1.toString() + "\n" + f2.toString() + "\n");
              j++;
            }
            counter++;
          }

          // Handle remaining direct flights
          while (i < directFlights.size()) {
            Flight f1 = directFlights.get(i);
            List<Flight> mapValue = new ArrayList<>();
            mapValue.add(f1);
            itineraries.put(counter, mapValue);
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
            List<Flight> mapValue = new ArrayList<>();
            mapValue.add(f1);
            mapValue.add(f2);
            itineraries.put(counter, mapValue);
            int indirectFlightTime = f1.time + f2.time;
            sb.append("Itinerary " + counter + ": 2 flight(s), " + indirectFlightTime + " minutes\n" + f1.toString()
                + "\n" + f2.toString() + "\n");
            j++;
            counter++;
          }

        } else {
          int itinCounter = 0;
          for (Flight flight : directFlights) {
            List<Flight> mapValue = new ArrayList<>();
            mapValue.add(flight);
            itineraries.put(itinCounter, mapValue);
            Flight f = flight;
            sb.append("Itinerary " + itinCounter + ": 1 flight(s), " + f.time
                + " minutes\n" + f.toString() + "\n");
            itinCounter++;

          }
        }
      } else {
        if (num == 0) {
          return "No flights match your selection\n";
        }
        int itinCounter = 0;
        for (Flight flight : directFlights) {
          Flight f = flight;
          List<Flight> mapValue = new ArrayList<>();
          mapValue.add(flight);
          itineraries.put(itinCounter, mapValue);
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

  /* See QueryAbstract.java for javadoc */
  public String transaction_book(int itineraryId) {
    // check if a user is logged in first
    if (currentLoggedInUser != null) {
      // user is logged in
      if (itineraryId < 0) {
        return "No such itinerary " + itineraryId + "\n";
      }
      try {
        conn.setAutoCommit(false);

        if (itineraries == null) {
          conn.rollback();
          conn.setAutoCommit(true);
          return "No such itinerary " + itineraryId + "\n";
        }
        // iterate through the itineraries map and check if the itineraryId
        // passed in is valid
        for (int key : itineraries.keySet()) {
          if (key == itineraryId) {
            // book it and then return a successful book string
            // if the person has a booking
            userResStmt.clearParameters();
            userResStmt.setString(1, this.currentLoggedInUser);
            ResultSet reserveResult = userResStmt.executeQuery();

            List<Flight> itineraryFlights = itineraries.get(key);
            Set<Integer> bookingDays = new HashSet<>();

            // Iterate through all flights in the current itinerary and add their days to a
            // set
            for (Flight flight : itineraryFlights) {
              bookingDays.add(flight.dayOfMonth);
            }

            // Iterate through the ResultSet to check for day conflicts
            while (reserveResult.next()) {
              int fid1 = reserveResult.getInt("fid1");
              int dayOfMonth1 = getDayOfMonth(fid1);
              if (bookingDays.contains(dayOfMonth1)) {
                reserveResult.close();
                conn.rollback();
                conn.setAutoCommit(true);
                return "You cannot book two flights in the same day\n";
              }

              int fid2 = reserveResult.getInt("fid2");
              if (fid2 != 0) {
                int dayOfMonth2 = getDayOfMonth(fid2);
                if (bookingDays.contains(dayOfMonth2)) {
                  reserveResult.close();
                  conn.rollback();
                  conn.setAutoCommit(true);
                  return "You cannot book two flights in the same day\n";
                }
              }
            }

            reserveResult.close();

            int countRes;

            if (itineraryFlights.size() > 1) {
              countRes = totalReservations(itineraryFlights.get(0).fid, itineraryFlights.get(1).fid);
            } else {
              countRes = totalReservations(itineraryFlights.get(0).fid, 0);
            }

            if (checkFlightCapacity(itineraryFlights.get(0).fid) - countRes <= 0) {
              conn.rollback();
              conn.setAutoCommit(true);
              return "Booking failed\n";
            }

            if (itineraryFlights.size() > 1) {
              if (checkFlightCapacity(itineraryFlights.get(1).fid) - countRes <= 0) {
                conn.rollback();
                conn.setAutoCommit(true);
                return "Booking failed\n";
              }
            }

            int totalReservations = getNumReservations();
            totalReservations += 1;

            insertResStmt.clearParameters();

            insertResStmt.setInt(1, totalReservations);
            insertResStmt.setString(2, this.currentLoggedInUser);
            insertResStmt.setInt(3, itineraryFlights.get(0).fid);
            if (itineraryFlights.size() > 1) {
              insertResStmt.setInt(4, itineraryFlights.get(1).fid);
            } else {
              insertResStmt.setNull(4, Types.INTEGER);
            }
            insertResStmt.setInt(5, 0);
            insertResStmt.executeUpdate();

            String res = "Booked flight(s), reservation ID: " + totalReservations
                + "\n";
            conn.commit();
            conn.setAutoCommit(true);
            return res;

          }
        }
        // if I went through the whole for loop and they didn't have the
        // itineraryId I wanted, then I return a nonexistent id booking
        conn.rollback();
        conn.setAutoCommit(true);
        return "No such itinerary " + itineraryId + "\n";
      } catch (SQLException e) {
        e.printStackTrace();
        try {
          conn.rollback();
          conn.setAutoCommit(true);

        } catch (SQLException e2) {
          e2.printStackTrace();
        }
        if (isDeadlock(e)) {
          return transaction_book(itineraryId);
        }
        return "Booking failed\n";
      }
    }
    // user is not logged in
    return "Cannot book reservations, not logged in\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_pay(int reservationId) {
    if (currentLoggedInUser == null) {
      return "Cannot pay, not logged in\n";
    }
    try {
      conn.setAutoCommit(false);
      resIdStmt.clearParameters();
      resIdStmt.setString(1, this.currentLoggedInUser);
      resIdStmt.setInt(2, reservationId);
      ResultSet reservations = resIdStmt.executeQuery();
      if (!reservations.isBeforeFirst()) {
        reservations.close();
        conn.rollback();
        conn.setAutoCommit(true);
        return "Cannot find unpaid reservation " + reservationId + " under user: "
            + currentLoggedInUser + "\n";
      }
      reservations.next();
      int status = reservations.getInt("status");
      if (status == 1) {
        reservations.close();
        conn.rollback();
        conn.setAutoCommit(true);
        return "Cannot find unpaid reservation " + reservationId + " under user: "
            + currentLoggedInUser + "\n";
      }
      balanceStmt.clearParameters();
      balanceStmt.setString(1, this.currentLoggedInUser);
      ResultSet balanceSet = balanceStmt.executeQuery();
      balanceSet.next();
      int balance = balanceSet.getInt("Balance");
      balanceSet.close();
      if (reservations.getInt("fid2") != 0) {
        int fid1 = reservations.getInt("fid1");
        int fid2 = reservations.getInt("fid2");
        int price = getPrice(fid1) + getPrice(fid2);
        if (balance < price) {
          conn.rollback();
          conn.setAutoCommit(true);
          return "User has only " + balance + " in account but itinerary costs "
              + price + "\n";
        } else {
          int remainingBalance = balance - price;
          // update the users balance by connecting to the database
          // String loginUpdateQuery = "UPDATE Users_lshibly SET LoggedIn = 1 WHERE
          // Username = ?;";
          updateBalanceStmt.clearParameters();

          updateBalanceStmt.setInt(1, remainingBalance);
          updateBalanceStmt.setString(2, currentLoggedInUser);

          updateBalanceStmt.executeUpdate();

          updateResStatusStmt.clearParameters();
          updateResStatusStmt.setInt(1, reservationId);

          updateResStatusStmt.executeUpdate();

          conn.commit();
          conn.setAutoCommit(true);
          return "Paid reservation: " + reservationId + " remaining balance: " +
              remainingBalance + "\n";
        }
      } else {
        int fid = reservations.getInt("fid1");
        int price = getPrice(fid);
        // check if user has enough balance to pay for the reservation
        if (balance < price) {
          conn.rollback();
          conn.setAutoCommit(true);
          return "User has only " + balance + " in account but itinerary costs "
              + price + "\n";
        } else {
          int remainingBalance = balance - price;
          // update the users balance by connecting to the database
          // update the users balance by connecting to the database
          // String loginUpdateQuery = "UPDATE Users_lshibly SET LoggedIn = 1 WHERE
          // Username = ?;";
          updateBalanceStmt.clearParameters();
          updateBalanceStmt.setInt(1, remainingBalance);
          updateBalanceStmt.setString(2, currentLoggedInUser);

          updateBalanceStmt.executeUpdate();

          updateResStatusStmt.clearParameters();
          updateResStatusStmt.setInt(1, reservationId);

          updateResStatusStmt.executeUpdate();

          conn.commit();
          conn.setAutoCommit(true);
          return "Paid reservation: " + reservationId + " remaining balance: " +
              remainingBalance + "\n";
        }
      }
    } catch (SQLException e) {
      try {
        conn.rollback();
        conn.setAutoCommit(true);
      } catch (SQLException e2) {
        e2.printStackTrace();
      }
      e.printStackTrace();
      return "Failed to pay for reservation " + reservationId + "\n";
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_reservations() {
    if (currentLoggedInUser == null) {
      return "Cannot view reservations, not logged in\n";
    }
    try {
      selectResStmt.clearParameters();
      selectResStmt.setString(1, currentLoggedInUser);
      ResultSet reservationsResult = selectResStmt.executeQuery();
      if (!reservationsResult.isBeforeFirst()) {
        return "No reservations found\n";
      }
      StringBuffer sb = new StringBuffer();
      while (reservationsResult.next()) {
        int resId = reservationsResult.getInt("ReservationID");
        sb.append("Reservation " + resId + " paid: ");
        if (reservationsResult.getInt("status") == 1) {
          sb.append("true:\n");
        } else {
          sb.append("false:\n");
        }
        int fid1 = reservationsResult.getInt("fid1");
        int fid2 = reservationsResult.getInt("fid2");
        Flight f1 = new Flight(fid1, getDayOfMonth(fid1), getCarrierId(fid1), getFlightNum(fid1), getOriginCity(fid1),
            getDestCity(fid1), getTime(fid1), checkFlightCapacity(fid1), getPrice(fid1));
        sb.append(f1.toString() + "\n");
        if (fid2 != 0) {
          Flight f2 = new Flight(fid2, getDayOfMonth(fid2), getCarrierId(fid2), getFlightNum(fid2), getOriginCity(fid2),
              getDestCity(fid2), getTime(fid2), checkFlightCapacity(fid2), getPrice(fid2));
          sb.append(f2.toString() + "\n");
        }
      }
      reservationsResult.close();
      return sb.toString();
    } catch (SQLException e) {
      e.printStackTrace();
      return "Failed to retrieve reservations\n";
    }
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
   * Searches for the flight and gives the price for that flight
   *
   * @param fid the flight id
   * @return the price for that flight
   */
  private int getPrice(int fid) {
    try {
      priceStmt.clearParameters();
      priceStmt.setInt(1, fid);
      ResultSet priceResult = priceStmt.executeQuery();
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
  private int getDayOfMonth(int fid) {
    try {
      dayStmt.clearParameters();
      dayStmt.setInt(1, fid);
      ResultSet dayOfMonthResult = dayStmt.executeQuery();
      dayOfMonthResult.next();
      int dayOfMonth = dayOfMonthResult.getInt("day_of_month");
      dayOfMonthResult.close();
      return dayOfMonth;
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
  private String getDestCity(int fid) {
    try {
      destStmt.clearParameters();
      destStmt.setInt(1, fid);
      ResultSet destCityResult = destStmt.executeQuery();
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
   * Searches for the specific flight and gives the origin city for that flight
   *
   * @param fid the flight id
   * @return the origin city for that flight
   */
  private String getOriginCity(int fid) {
    try {
      originStmt.clearParameters();
      originStmt.setInt(1, fid);
      ResultSet originCityResult = originStmt.executeQuery();
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
   * Searches for the specific flight and gives the carrier for that flight
   *
   * @param fid the flight id
   * @return the carrier for that flight
   */
  private String getCarrierId(int fid) {
    try {
      carrierStmt.clearParameters();
      carrierStmt.setInt(1, fid);
      ResultSet carrierIdResult = carrierStmt.executeQuery();
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
      flightNumStmt.clearParameters();
      flightNumStmt.setInt(1, fid);
      ResultSet flightNumResult = flightNumStmt.executeQuery();
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
      timeStmt.clearParameters();
      timeStmt.setInt(1, fid);
      ResultSet timeResult = timeStmt.executeQuery();
      timeResult.next();
      int time = timeResult.getInt("actual_time");
      timeResult.close();
      return time;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  private int getNumReservations() {
    try {
      resStmt.clearParameters();
      ResultSet res = resStmt.executeQuery();
      int num = 0;
      while (res.next()) {
        num = res.getInt("num");
      }
      return num;
    } catch (SQLException e) {
      e.printStackTrace();
      return -1;
    }
  }

  private boolean hasReserved(String username, int fid1, int fid2) {
    try {
      hasStmt.clearParameters();
      hasStmt.setString(1, username);
      hasStmt.setInt(2, fid1);
      hasStmt.setInt(3, fid2);

      ResultSet reservation = hasStmt.executeQuery();
      boolean result = false;
      while (reservation.next()) {
        result = true;
      }
      return result;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  private int totalReservations(int fid1, int fid2) {
    try {
      int count = 0;

      totalStmt.clearParameters();

      totalStmt.setInt(1, fid1);
      totalStmt.setInt(2, fid2);

      ResultSet res = totalStmt.executeQuery();

      while (res.next()) {
        count = res.getInt("num");
      }

      return count;

    } catch (SQLException e) {
      e.printStackTrace();
      return -1;
    }
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