# Проект "Animal-shelter"

Данный проект направлен на автоматизацию работы приюта животных с клиентами. Целью проекта является максимальное снятие нагрузки с персонала приюта, переложив ее на telegram-бота. Пользователям нашего проекта будет доступна следующая функциональность бота:

- получение пользователем разного рода информации по работе приюта.
  Информация хранится в ресурсах приложения и легко может быть
  скорректирована. Посылать клиенту можно разные типы информации:
  тексты, фотографии, файлы;
- пользователь может просматривать описания питомцев с их фотографиями;
- функция выбора питомца пользователем с оформлением испытательного срока;
- функция по ведению питомца на  испытательном сроке;
- возможность стать волонтером через приложение;
- организация чата между пользователем и волонтером;
- заказ обратного звонка пользователю (при невозможности быстрого
  ответа есть возможность указать пользователю запланированное время
  обратного звонка).

*Для запуска приложения необходимо:*
1) подготовить пустую базу данных postgresql в кодировке UTF-8;
2) заполнить следующие параметры приложения:
   * а) доступ к рабочей базе:
     * spring.datasource.url
     * spring.datasource.username
     * spring.datasource.password
   * б)  путь к папке, где будут храниться отчеты пользователя по
   испытательному сроку и фотографии питомцев. Пример:
     * shetler.storage.full-path=/C:/Storage/AnimalShetler/
   * в) для тестирования следует настроить доступ к тестовой базе данных 
   в отдельном файле application.properties, предварительно создав
   пустую базу postgresql в кодировке UTF-8.

*Особенности приложения:*
- приложение многопоточное (работа с каждым пользователем ведется в
  отдельном потоке);

- через функции контроллера доступно оформление администраторских
  записей в базе данных и просмотр всех пользователей, записанных в базу
  данных. Вся остальная функциональность происходит через работу с
  ботом;

- разработан специальный механизм проведения опроса пользователя в
  диалоговом режиме. Например, необходимо расширить функциональность,
  написав новую функцию к проекту. Но для работы бизнес-логики этой
  функции требуется получить дополнительные данные от клиента. Тогда сам
  диалог опроса предварительно создается в xml файле в удобном для
  человека формате (с возможностью проверки введенной пользователем
  информации с помощью регулярных выражений). В новой функции достаточно
  запустить процесс подготовленного опроса, а на выходе будем иметь
  необходимые данные, полученные от клиента и которые можно использовать
  далее в коде этой функции. Таким образом, с разработчика полностью
  снимается нагрузка по вопросу получения данных от пользователя. Сам
  механизм опроса построен на базе интерфейса Askable. Поэтому при
  желании разработчик может реализовать по своему данный интерфейс и
  получить более гибкую конструкцию проведения опроса (например,
  потребуется создать сложную многоуровневую анкету).